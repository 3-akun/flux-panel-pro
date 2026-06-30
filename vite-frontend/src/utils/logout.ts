/**
 * 安全退出登录函数
 * 只清除认证相关数据，保留主题、面板地址等用户偏好。
 */
export const safeLogout = () => {
  [
    'token',
    'role_id',
    'name',
    'admin',
    'e',
    'lastNotified',
  ].forEach((key) => localStorage.removeItem(key));
}; 