import request from './request'
import type { Result, LoginResponse } from '@/types'

export function login(username: string, password: string) {
  return request.post('/auth/login', { username, password }) as Promise<Result<LoginResponse>>
}

export function register(username: string, password: string) {
  return request.post('/auth/register', { username, password }) as Promise<Result<{ userId: number; username: string }>>
}
