export const mvpScope = {
  title: '第一版个人自用 MVP',
  summary: '仅聚焦个人自用 TCP/UDP 中转，不包含商城、支付、套餐、自动续费和公开用户系统。',
  includedFeatures: [
    'TCP/UDP 中转',
    '隧道管理',
    '链路探测',
    '限速',
    '流量统计',
  ],
  excludedFeatures: [
    '商城',
    '支付',
    '套餐',
    '自动续费',
    '公开用户系统',
  ],
} as const;
