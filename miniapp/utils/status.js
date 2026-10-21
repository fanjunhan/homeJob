/**
 * 工单状态归一化与展示映射。
 *
 * 背景：后端 ScheduleStatus 枚举值为大写（SCHEDULED / FINISHED / EVALUATED），
 * JPA 默认按 Enum.name() 存储，接口返回即大写。前端早期代码用小写字符串比较，
 * 导致 s.status === 'finished' 永远为 false——待确认列表渲染不出、签名与评价入口不显示。
 *
 * 这里的策略是大小写无关地归一化，无论后端返回 'FINISHED' 还是 'finished' 都能匹配，
 * 避免再次因为服务端序列化细节变化而失效。
 */

/** 归一化为小写（服务端可能返回大写枚举名） */
function normalize(status) {
  return String(status || '').toLowerCase();
}

const STATUS_TEXT = {
  scheduled: '已排班',
  notified: '已通知',
  accepted: '已接单',
  in_service: '服务中',
  finished: '待确认',
  signed: '待评价',
  evaluated: '已完成',
  cancelled: '已取消',
  reassigned: '改派中'
};

/** 状态徽标颜色（与 app.wxss 的 .st-* 对应） */
const STATUS_THEME = {
  scheduled: 'st-scheduled',
  notified: 'st-notified',
  accepted: 'st-accepted',
  in_service: 'st-in_service',
  finished: 'st-finished',
  signed: 'st-signed',
  evaluated: 'st-evaluated',
  cancelled: 'st-cancelled',
  reassigned: 'st-scheduled'
};

/** 需要客户动作的状态：签名或评价 */
const PENDING_CUSTOMER = ['finished', 'signed'];

/** 已完结状态 */
const DONE = ['evaluated', 'cancelled'];

/**
 * 归一化工单列表的 status 字段，并附加 statusText / statusTheme 供视图直接渲染。
 * 这样 WXML 里就不必写一串三元表达式。
 */
function decorate(list) {
  if (!Array.isArray(list)) return [];
  return list.map((item) => {
    const s = normalize(item.status);
    return Object.assign({}, item, {
      status: s,
      statusText: STATUS_TEXT[s] || '未知状态',
      statusTheme: STATUS_THEME[s] || 'st-scheduled'
    });
  });
}

/**
 * 判断客户是否需要执行动作。
 * 只有 'finished'（待签名）与 'signed'（待评价）会进入待确认列表。
 */
function isPending(s) {
  return PENDING_CUSTOMER.indexOf(normalize(s)) >= 0;
}

function isDone(s) {
  return DONE.indexOf(normalize(s)) >= 0;
}

module.exports = {
  normalize,
  decorate,
  isPending,
  isDone,
  STATUS_TEXT,
  STATUS_THEME,
  PENDING_CUSTOMER,
  DONE
};
