export interface LoginRequest {
  username: string;
  password: string;
  rememberMe?: boolean;
}

export interface LoginResponse {
  accessToken: string;
  username:    string;
  displayName: string;
  role:        string;
}

export interface UserInfo {
  username:    string;
  displayName: string;
  role:        string;
}

export interface UserResponse {
  uuid:        string;
  username:    string;
  email:       string;
  displayName: string;
  role:        string;
  enabled:     boolean;
  createdAt:   string;
}

export interface CreateUserRequest {
  username:    string;
  displayName: string;
  email:       string;
  password:    string;
}
