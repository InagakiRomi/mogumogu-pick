const ROLE_LABELS: Record<number, string> = {
  0: '系統管理員',
  1: '群組管理者',
  2: '一般使用者',
}

export function getRoleLabel(role?: number | null) {
  if (role == null) {
    return '未知角色'
  }

  return ROLE_LABELS[role] ?? '未知角色'
}

export function isRegularUser(role?: number | null) {
  return role === 2
}

export function isGroupAdmin(role?: number | null) {
  return role === 1
}
