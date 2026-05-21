package com.tasknotes.concepts.service;

import com.tasknotes.concepts.dto.AcceptConceptRequest;
import com.tasknotes.concepts.dto.ConceptSuggestionResponse;
import com.tasknotes.concepts.model.ProgrammingConcept;
import com.tasknotes.concepts.repository.ProgrammingConceptRepository;
import com.tasknotes.users.service.SecurityHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProgrammingConceptService {

    private static final Logger log = LoggerFactory.getLogger(ProgrammingConceptService.class);

    private static final int MAX_TERM_LENGTH = 80;
    private static final int MIN_TERM_LENGTH = 2;
    private static final int CACHE_MAX_SIZE  = 300;

    // Cache covers only GLOBAL concepts (scope-independent for seeds)
    private final ConcurrentHashMap<String, Optional<ConceptSuggestionResponse>> globalCache =
            new ConcurrentHashMap<>();

    private final ProgrammingConceptRepository repository;
    private final SecurityHelper securityHelper;

    public ProgrammingConceptService(ProgrammingConceptRepository repository,
                                     SecurityHelper securityHelper) {
        this.repository = repository;
        this.securityHelper = securityHelper;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public ConceptSuggestionResponse suggest(String rawTerm, boolean semicolonTrigger) {
        if (rawTerm == null || rawTerm.isBlank()) return ConceptSuggestionResponse.notFound();

        String term       = rawTerm.trim();
        String normalized = normalize(term);

        if (normalized.length() < MIN_TERM_LENGTH || normalized.length() > MAX_TERM_LENGTH) {
            return ConceptSuggestionResponse.notFound();
        }

        long start = System.currentTimeMillis();

        // ── 1. Global cache hit (covers GLOBAL scope only) ───────────────────
        boolean globalAlreadyChecked = false;
        if (globalCache.containsKey(normalized)) {
            Optional<ConceptSuggestionResponse> cached = globalCache.get(normalized);
            if (cached.isPresent()) {
                // Positive hit: found in GLOBAL scope
                return cached.get();
            }
            // Negative hit: not in GLOBAL scope — skip DB queries for GLOBAL,
            // but still fall through to check USER scope for the current user
            globalAlreadyChecked = true;
        }

        if (!globalAlreadyChecked) {
            // ── 2. Exact match — GLOBAL scope ─────────────────────────────────
            Optional<ProgrammingConcept> exact = repository.findByNormalizedTermAndScope(normalized, "GLOBAL");
            if (exact.isPresent()) {
                var resp = ConceptSuggestionResponse.of(exact.get(), "LOCAL");
                putGlobalCache(normalized, Optional.of(resp));
                log.info("concept_semicolon_lookup_completed termNormalized={} source=LOCAL scope=GLOBAL found=true durationMs={}",
                        normalized, System.currentTimeMillis() - start);
                return resp;
            }

            // ── 3. Prefix match — GLOBAL scope ────────────────────────────────
            var prefixMatches = repository.findByNormalizedTermStartingWithAndScope(normalized, "GLOBAL", PageRequest.of(0, 1));
            if (!prefixMatches.isEmpty()) {
                var resp = ConceptSuggestionResponse.of(prefixMatches.get(0), "LOCAL");
                putGlobalCache(normalized, Optional.of(resp));
                log.info("concept_semicolon_lookup_completed termNormalized={} source=LOCAL scope=GLOBAL found=true durationMs={}",
                        normalized, System.currentTimeMillis() - start);
                return resp;
            }

            // Not in GLOBAL scope — record in cache so next request skips GLOBAL DB queries
            putGlobalCache(normalized, Optional.empty());
        }

        // ── 4. Exact match — current user's USER scope ────────────────────────
        try {
            Long ownerId = securityHelper.currentUserId();
            Optional<ProgrammingConcept> userExact = repository.findByNormalizedTermAndOwnerUserId(normalized, ownerId);
            if (userExact.isPresent()) {
                log.info("concept_semicolon_lookup_completed termNormalized={} source=LOCAL scope=USER found=true durationMs={}",
                        normalized, System.currentTimeMillis() - start);
                return ConceptSuggestionResponse.of(userExact.get(), "LOCAL");
            }
        } catch (IllegalStateException ignored) {
            // unauthenticated context (shouldn't happen with secured endpoints)
        }

        // ── 5. Heuristic: reject non-technical terms ──────────────────────────
        if (!semicolonTrigger && !isTechnicalTerm(term)) {
            log.info("concept_suggestion_rejected termNormalized={} reason=not_technical_term", normalized);
            return ConceptSuggestionResponse.notFound();
        }

        log.info("concept_semicolon_lookup_not_found termNormalized={} durationMs={}",
                normalized, System.currentTimeMillis() - start);
        return ConceptSuggestionResponse.notFound();
    }

    @Transactional
    public void accept(AcceptConceptRequest req) {
        String normalized = normalize(req.term());
        if (normalized.isBlank() || req.summary().isBlank()) return;

        String source = req.source() != null ? req.source() : "USER";

        // If a GLOBAL concept exists, increment its counter
        Optional<ProgrammingConcept> global = repository.findByNormalizedTermAndScope(normalized, "GLOBAL");
        if (global.isPresent()) {
            ProgrammingConcept existing = global.get();
            existing.setAcceptedCount(existing.getAcceptedCount() + 1);
            existing.setLastUsedAt(LocalDateTime.now());
            repository.save(existing);
            log.info("concept_accept_updated termNormalized={} scope=GLOBAL source={}", normalized, source);
            return;
        }

        // Create or update USER-scoped concept
        Long ownerId = null;
        try {
            ownerId = securityHelper.currentUserId();
        } catch (IllegalStateException ignored) {
            // system context — treat as GLOBAL
        }

        final Long finalOwnerId = ownerId;
        Optional<ProgrammingConcept> existing = finalOwnerId != null
                ? repository.findByNormalizedTermAndOwnerUserId(normalized, finalOwnerId)
                : Optional.empty();

        if (existing.isPresent()) {
            ProgrammingConcept c = existing.get();
            c.setAcceptedCount(c.getAcceptedCount() + 1);
            c.setLastUsedAt(LocalDateTime.now());
            repository.save(c);
            log.info("concept_accept_updated termNormalized={} scope=USER", normalized);
        } else {
            ProgrammingConcept c = new ProgrammingConcept();
            c.setTerm(req.term().trim());
            c.setNormalizedTerm(normalized);
            c.setSummary(req.summary().trim());
            c.setSource(source);
            c.setSourceUrl(req.sourceUrl());
            c.setAcceptedCount(1);
            c.setScope(finalOwnerId != null ? "USER" : "GLOBAL");
            c.setOwnerUserId(finalOwnerId);
            repository.save(c);
            log.info("concept_accept_created termNormalized={} scope={}", normalized,
                    finalOwnerId != null ? "USER" : "GLOBAL");
        }
    }

    // ── Seed ──────────────────────────────────────────────────────────────────

    @EventListener(ApplicationReadyEvent.class)
    public void seedIfEmpty() {
        // Only seed if no GLOBAL concepts exist; USER-scope records must not prevent seeding
        if (repository.existsByScope("GLOBAL")) return;
        log.info("concept_seed_started count={}", SEED_DATA.length);
        int inserted = 0;
        for (String[] entry : SEED_DATA) {
            String normalized = normalize(entry[0]);
            // Skip if any concept with this normalizedTerm already exists (avoid unique constraint violation)
            if (repository.findByNormalizedTerm(normalized).isPresent()) {
                log.debug("concept_seed_skipped term={} reason=already_exists", entry[0]);
                continue;
            }
            try {
                ProgrammingConcept c = new ProgrammingConcept();
                c.setTerm(entry[0]);
                c.setNormalizedTerm(normalized);
                c.setSummary(entry[1]);
                c.setSource("SEED");
                c.setScope("GLOBAL");
                c.setAcceptedCount(0);
                repository.save(c);
                inserted++;
            } catch (Exception e) {
                log.warn("concept_seed_entry_failed term={} reason={}", entry[0], e.getMessage());
            }
        }
        log.info("concept_seed_completed inserted={}", inserted);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    static String normalize(String term) {
        if (term == null) return "";
        return term.toLowerCase(java.util.Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    private void putGlobalCache(String key, Optional<ConceptSuggestionResponse> value) {
        if (globalCache.size() >= CACHE_MAX_SIZE) globalCache.clear();
        globalCache.put(key, value);
    }

    // ── Technical term heuristic ──────────────────────────────────────────────

    private static final Set<String> STOPWORDS = Set.copyOf(List.of(
        "de","da","do","dos","das","um","uma","uns","umas","o","a","os","as",
        "e","em","no","na","nos","nas","se","que","para","com","por","mas",
        "ou","nem","sim","não","já","só","também","mais","menos","muito",
        "pouco","bem","mal","todo","toda","todos","todas","este","esta",
        "esse","essa","isso","aqui","ali","lá","é","foi","ser","ter",
        "the","be","is","are","was","were","been","being","have","has","had",
        "do","does","did","will","would","can","could","may","might","shall",
        "should","and","or","but","nor","for","yet","so","in","on","at","to",
        "of","by","as","from","with","about","this","that","these","those",
        "it","its","he","she","they","we","you","i","me","him","her",
        "not","no","yes","if","then","when","where","how","what","who","which"
    ));

    private static final Set<String> TECH_KEYWORDS = Set.copyOf(List.of(
        "java","python","javascript","typescript","golang","go","rust","kotlin",
        "swift","scala","ruby","php","perl","bash","shell","powershell",
        "spring","angular","react","vue","node","express","django","flask",
        "docker","kubernetes","aws","azure","gcp","terraform","ansible",
        "sql","nosql","mysql","postgres","postgresql","mongodb","redis",
        "kafka","rabbitmq","elasticsearch","cassandra","sqlite","h2",
        "git","github","gitlab","jenkins","ci","cd","devops","agile","scrum",
        "api","rest","graphql","grpc","http","https","websocket","tcp","udp",
        "json","xml","yaml","yml","csv","html","css","scss","sass",
        "array","hash","map","stack","queue","tree","graph","heap","list",
        "sort","search","algorithm","recursion","iteration","loop",
        "class","object","interface","abstract","enum","generic",
        "inheritance","polymorphism","encapsulation","abstraction",
        "singleton","factory","observer","decorator","builder","proxy",
        "pattern","mvc","mvvm","mvp","ddd","tdd","bdd","solid","kiss","dry",
        "orm","jpa","jdbc","hibernate","entity","repository","service",
        "jwt","oauth","ssl","tls","cors","csrf","xss","auth","token",
        "lambda","closure","functional","reactive","async","await",
        "thread","concurrent","mutex","semaphore","deadlock","race",
        "microservice","monolith","serverless","container","pod","cluster",
        "cache","queue","broker","message","event","stream","batch",
        "pojo","dto","dao","bean","component","controller","model",
        "uuid","guid","hash","base64","encode","decode","encrypt","decrypt",
        "cqrs","event","sourcing","saga","circuit","breaker","retry",
        "regex","parser","lexer","compiler","interpreter","runtime",
        "deploy","pipeline","artifact","build","test","lint","coverage",
        "proxy","gateway","balancer","cdn","dns","vpc","subnet","firewall",
        "n8n","webhook","integration","automation","workflow","pipeline"
    ));

    boolean isTechnicalTerm(String rawTerm) {
        String term = rawTerm.trim();
        if (term.length() < MIN_TERM_LENGTH) return false;
        String lower = term.toLowerCase(java.util.Locale.ROOT);
        if (STOPWORDS.contains(lower)) return false;
        if (term.matches("[A-Z]{2,}")) return true;
        if (!term.contains(" ") && term.matches("[A-Za-z0-9]+") && term.matches(".*\\d.*")) return true;
        if (term.matches("[A-Z]+(/[A-Z]+)+")) return true;
        for (String word : lower.split("[\\s/\\-]+")) {
            if (!word.isEmpty() && TECH_KEYWORDS.contains(word)) return true;
        }
        return TECH_KEYWORDS.contains(lower);
    }

    // ── Seed data ─────────────────────────────────────────────────────────────

    private static final String[][] SEED_DATA = {
        {"JSON",        "JavaScript Object Notation; formato leve de troca de dados legível por humanos e máquinas, amplamente usado em APIs REST."},
        {"UUID",        "Universally Unique Identifier; identificador de 128 bits gerado para ser globalmente único sem coordenação central."},
        {"UUID v7",     "UUID da versão 7; ordenável por tempo (timestamp + random), ideal para chaves primárias em bancos modernos."},
        {"POJO",        "Plain Old Java Object; classe Java simples que representa dados sem depender de frameworks ou bibliotecas específicas."},
        {"DTO",         "Data Transfer Object; objeto usado para transferir dados entre camadas ou sistemas, sem lógica de negócio."},
        {"DAO",         "Data Access Object; padrão que abstrai o acesso ao banco de dados, separando persistência da lógica de negócio."},
        {"REST",        "Representational State Transfer; estilo arquitetural para APIs que usa HTTP com recursos nomeados por URLs e verbos semânticos."},
        {"API",         "Application Programming Interface; conjunto de definições e protocolos que permite integração entre softwares distintos."},
        {"HTTP",        "HyperText Transfer Protocol; protocolo de comunicação base da web para transferência de dados entre cliente e servidor."},
        {"HTTPS",       "HTTP Secure; versão segura do HTTP que usa TLS para criptografar toda a comunicação entre cliente e servidor."},
        {"CORS",        "Cross-Origin Resource Sharing; mecanismo HTTP que controla quais origens externas podem acessar recursos de um servidor."},
        {"JWT",         "JSON Web Token; padrão compacto para transmitir informações assinadas entre partes, muito usado em autenticação stateless."},
        {"ORM",         "Object-Relational Mapping; técnica que converte entre objetos de linguagens OO e registros de bancos de dados relacionais."},
        {"JPA",         "Java Persistence API; especificação Java para mapeamento objeto-relacional e gerenciamento de persistência em bancos SQL."},
        {"Hibernate",   "Framework ORM para Java que implementa JPA; mapeia classes Java a tabelas de banco e gerencia o ciclo de vida das entidades."},
        {"Spring Boot", "Framework Java que simplifica a criação de aplicações Spring com auto-configuração e servidor embutido, pronto para produção."},
        {"TypeScript",  "Superconjunto tipado do JavaScript que adiciona tipos estáticos e recursos modernos, compilando para JavaScript puro."},
        {"JavaScript",  "Linguagem de programação interpretada e multiparadigma; a única linguagem nativa dos navegadores, também usada no backend via Node.js."},
        {"Array",       "Estrutura de dados que armazena elementos em posições contíguas de memória, acessados por índice numérico em tempo O(1)."},
        {"Bubble Sort", "Algoritmo de ordenação O(n²) que percorre a lista repetidamente trocando elementos adjacentes fora de ordem; simples mas ineficiente."},
        {"Docker",      "Plataforma de containerização que empacota aplicação e dependências em contêineres portáteis, isolados e reproduzíveis."},
        {"Kubernetes",  "Sistema de orquestração de contêineres (K8s) que automatiza implantação, escalonamento e gerenciamento de aplicações containerizadas."},
        {"N8N",         "Plataforma open-source de automação de fluxo de trabalho low-code que integra centenas de serviços e APIs via nós visuais."},
        {"CQRS",        "Command Query Responsibility Segregation; padrão que separa comandos (escrita) de queries (leitura) em modelos distintos."},
        {"DDD",         "Domain-Driven Design; abordagem de desenvolvimento que centraliza o design no modelo do domínio de negócio com linguagem ubíqua."},
        {"SOLID",       "Cinco princípios OOP: Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation e Dependency Inversion."},
        {"SQL",         "Structured Query Language; linguagem padrão para criação, consulta e manipulação de bancos de dados relacionais."},
        {"NoSQL",       "Categoria de bancos de dados não relacionais (MongoDB, Redis, Cassandra) otimizados para flexibilidade, escala ou performance."},
        {"Git",         "Sistema de controle de versão distribuído; rastreia alterações em arquivos e coordena trabalho colaborativo entre desenvolvedores."},
        {"TDD",         "Test-Driven Development; metodologia em que o teste é escrito antes do código de produção, guiando o design pela testabilidade."},
        {"MVC",         "Model-View-Controller; padrão arquitetural que separa dados (Model), interface (View) e lógica de controle (Controller)."},
        {"CRUD",        "Create, Read, Update, Delete; as quatro operações básicas de persistência de dados em qualquer sistema de armazenamento."},
        {"WebSocket",   "Protocolo bidirecional full-duplex sobre TCP; permite comunicação em tempo real entre cliente e servidor sem polling."},
        {"GraphQL",     "Linguagem de consulta para APIs onde o cliente especifica exatamente os dados que precisa, evitando over-fetching e under-fetching."},
        {"OAuth",       "Open Authorization; protocolo que permite a um serviço acessar recursos de outro em nome do usuário sem expor credenciais."},
        {"SSL",         "Secure Sockets Layer; protocolo criptográfico legado para comunicação segura na internet, substituído pelo TLS."},
        {"TLS",         "Transport Layer Security; protocolo criptográfico moderno que garante confidencialidade e integridade nas comunicações de rede."},
        {"XML",         "Extensible Markup Language; formato de marcação hierárquico para dados estruturados, legível por humanos e máquinas."},
        {"YAML",        "YAML Ain't Markup Language; formato de serialização de dados legível por humanos, amplamente usado em arquivos de configuração."},
        {"CSV",         "Comma-Separated Values; formato de arquivo simples para dados tabulares, onde campos são separados por vírgulas ou ponto-e-vírgula."},
        {"Regex",       "Regular Expression; sequência de caracteres que define um padrão de busca para correspondência, validação e manipulação de strings."},
        {"Lambda",      "Função anônima (sem nome) passada como valor; fundamental em programação funcional e usada em callbacks, streams e event handlers."},
        {"Closure",     "Função que captura e retém referências às variáveis do escopo léxico onde foi criada, mesmo após esse escopo ser encerrado."},
        {"Singleton",   "Padrão criacional que garante que uma classe tenha apenas uma instância em todo o programa, com acesso global a ela."},
        {"Factory",     "Padrão criacional que delega a criação de objetos a uma interface ou método, desacoplando o cliente da classe concreta."},
        {"Observer",    "Padrão comportamental em que um objeto (subject) notifica automaticamente seus dependentes (observers) sobre mudanças de estado."},
        {"Decorator",   "Padrão estrutural que adiciona responsabilidades a um objeto dinamicamente, sem alterar a classe original."},
        {"Interface",   "Contrato OOP que declara métodos e propriedades que uma classe deve implementar, sem fornecer implementação."},
        {"Redis",       "Banco de dados em memória de código aberto; usado como cache, filas de mensagens e armazenamento de sessão de ultra-baixa latência."},
        {"MongoDB",     "Banco de dados NoSQL orientado a documentos; armazena dados em formato BSON (JSON binário) com esquema flexível."},
        {"PostgreSQL",  "Banco de dados relacional open-source avançado, reconhecido por conformidade com SQL, extensibilidade e robustez em produção."},
        {"MySQL",       "Sistema de gerenciamento de banco de dados relacional open-source amplamente usado em aplicações web e stacks LAMP/LEMP."},
        {"Kafka",       "Plataforma de streaming de eventos distribuída da Apache; usada para pipelines de dados de alta vazão e integração assíncrona."},
        {"CI/CD",       "Continuous Integration/Continuous Delivery; práticas de automação que integram, testam e entregam código de forma frequente e confiável."},
        {"Agile",       "Conjunto de metodologias de desenvolvimento baseadas em ciclos iterativos curtos, colaboração e adaptação a mudanças."},
        {"Scrum",       "Framework ágil que organiza o trabalho em sprints de 1-4 semanas com papéis definidos (PO, SM, Dev) e cerimônias regulares."},
        {"Microservices","Estilo arquitetural que decompõe uma aplicação em serviços pequenos, implantáveis independentemente e com responsabilidades delimitadas."},
        {"Docker Compose","Ferramenta para definir e executar aplicações Docker multi-contêiner usando um arquivo YAML de configuração declarativo."},
        {"Recursão",    "Técnica em que uma função chama a si mesma para resolver subproblemas menores; requer caso base para evitar loop infinito."},
        {"HashMap",     "Estrutura de dados que mapeia chaves a valores usando uma função hash; oferece busca, inserção e remoção em O(1) amortizado."},
        {"Polimorfismo","Capacidade OOP de objetos de tipos diferentes responderem à mesma mensagem com comportamentos específicos de cada tipo."},
        {"Encapsulamento","Princípio OOP que agrupa dados e comportamentos em uma unidade (classe) e restringe acesso externo via modificadores de acesso."},
        {"Herança",     "Mecanismo OOP em que uma classe filha herda atributos e métodos da classe pai, promovendo reutilização e especialização."},
        {"Git Flow",    "Modelo de ramificação Git com branches dedicadas para features, releases e hotfixes, organizando o ciclo de release de software."},
        {"WebHook",     "Mecanismo em que um servidor envia automaticamente uma requisição HTTP a uma URL configurada quando um evento ocorre."},
        {"Proxy",       "Padrão ou componente intermediário que age em nome de outro objeto ou servidor, podendo adicionar cache, segurança ou balanceamento."},
        {"Big O",       "Notação matemática que descreve o comportamento assintótico (pior caso) de algoritmos em termos de tempo e espaço."},
        {"Thread",      "Unidade leve de execução dentro de um processo; threads do mesmo processo compartilham memória, exigindo sincronização cuidadosa."},
        {"CI",          "Continuous Integration; prática de integrar código ao repositório principal com frequência, com builds e testes automáticos a cada commit."},
        {"CD",          "Continuous Delivery ou Deployment; extensão do CI que automatiza a entrega ou implantação do software em ambientes de produção."},
        {"Kanban",      "Método ágil visual que gerencia o fluxo de trabalho usando colunas (A fazer, Em progresso, Concluído) e limites de WIP."},
        {"Redis Cache", "Uso do Redis como camada de cache in-memory para reduzir carga no banco de dados e melhorar tempo de resposta da aplicação."},
        {"REST API",    "Interface de programação que segue os princípios REST; usa verbos HTTP (GET, POST, PUT, DELETE) sobre recursos identificados por URLs."},
        {"JWT Token",   "JSON Web Token usado como credencial de autenticação; composto por header, payload e assinatura, transportado no header Authorization."},
    };
}
