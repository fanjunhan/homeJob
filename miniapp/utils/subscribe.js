/**
 * 小程序订阅消息工具。
 *
 * 微信订阅消息规则（自 2020 年起）：
 *  - 一次性订阅：用户在具体场景（点击按钮 / 完成某动作）授权一次，可发一条；
 *  - 长期订阅：仅对特定行业（如公共服务、医疗）开放，本场景多数走一次性；
 *  - 用户拒绝后，同一模板再次调用会直接返回 reject，且短期内无法重复弹窗骚扰。
 *
 * 因此策略是：把授权请求放在「用户确实关心通知」的时机（登录完成、服务完成），
 * 并在本地记录授权结果，避免反复弹窗导致体验下降与被平台限流。
 */

const AUTH_KEY = 'subscribe_auth_record';

/** 读取某个模板的授权记录：'accept' | 'reject' | 未授权 */
function getAuthState(tmplId) {
  const record = wx.getStorageSync(AUTH_KEY) || {};
  return record[tmplId] || '';
}

/** 记录授权结果 */
function setAuthState(tmplId, state) {
  const record = wx.getStorageSync(AUTH_KEY) || {};
  record[tmplId] = state;
  wx.setStorageSync(AUTH_KEY, record);
}

/**
 * 请求订阅授权。
 *
 * @param tmplIds 模板 ID 数组
 * @param onAccept 用户至少同意一个模板时回调
 * @param onReject 用户全部拒绝时回调（不阻塞主流程）
 */
function requestSubscribe(tmplIds, onAccept, onReject) {
  if (!wx.requestSubscribeMessage) {
    // 基础库过低时降级：跳过授权，不阻塞业务
    if (onReject) onReject();
    return;
  }

  // 已明确拒绝过的模板不再重复申请，避免骚扰
  const toRequest = tmplIds.filter((id) => getAuthState(id) !== 'reject');
  if (toRequest.length === 0) {
    if (onReject) onReject();
    return;
  }

  wx.requestSubscribeMessage({
    tmplIds: toRequest,
    success(res) {
      const accepted = [];
      const rejected = [];
      for (const id of toRequest) {
        // res[id] === 'accept' 表示用户同意接收该模板
        if (res[id] === 'accept') {
          accepted.push(id);
          setAuthState(id, 'accept');
        } else {
          rejected.push(id);
          setAuthState(id, 'reject');
        }
      }
      if (accepted.length > 0) {
        if (onAccept) onAccept(accepted);
      } else if (onReject) {
        onReject();
      }
    },
    fail() {
      // 用户关闭弹窗或接口异常：不打扰，直接走降级路径
      if (onReject) onReject();
    }
  });
}

/** 是否有任一模板处于已授权状态（供页面展示提示） */
function hasAnyAuth(tmplIds) {
  return tmplIds.some((id) => getAuthState(id) === 'accept');
}

/** 是否全部拒绝（用于提示用户去设置里手动开启） */
function allRejected(tmplIds) {
  return tmplIds.length > 0 && tmplIds.every((id) => getAuthState(id) === 'reject');
}

module.exports = {
  TEMPLATE_IDS: ['MINIAPP_TMPL_FINISH'],
  requestSubscribe,
  getAuthState,
  setAuthState,
  hasAnyAuth,
  allRejected
};
