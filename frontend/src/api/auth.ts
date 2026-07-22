import request from './request'
import type { Result, LoginResponse } from '@/types'

// 登录：POST /auth/login，返回 JWT（token）。M1 接口放行，登录主要用于演示流程
export function login(username: string, password: string) {
  return request.post('/auth/login', { username, password }) as Promise<Result<LoginResponse>>
}

// 注册：POST /auth/register（M1 为内存用户，仅演示）
export function register(username: string, password: string) {
  return request.post('/auth/register', { username, password }) as Promise<Result<{ userId: number; username: string }>>
}
