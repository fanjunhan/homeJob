const api = require('../../utils/api');
const { requestSubscribe } = require('../../utils/subscribe');

/** 订阅消息模板 ID：需在小程序后台申请后填入 */
const TEMPLATE_IDS = [
  'MINIAPP_TMPL_FINISH'
];

Page({
  data: {
    agree: false,
    loading: false
  },

  onLoad() {
    // 已登录直接进入首页
    if (wx.getStorageSync('token')) {
      wx.reLaunch({ url: '/pages/index/index' });
    }
  },

  toggleAgree() {
    this.setData({ agree: !this.data.agree });
  },

  /** 协议为占位页，实际应跳转到独立协议页或 webview */
  openProtocol() {
    wx.showToast({ title: '协议内容请在正式版本查看', icon: 'none' });
  },

  onGetPhone(e) {
    if (this.data.loading) return;
    if (!this.data.agree) {
      wx.showToast({ title: '请先阅读并同意用户协议与隐私政策', icon: 'none' });
      return;
    }
    // 新版 getPhoneNumber 回调：detail.code 为动态令牌，detail.encryptedData 为旧版字段
    const phoneCode = e.detail.code || e.detail.encryptedData || e.detail.iv;
    if (!phoneCode) {
      wx.showToast({ title: '需要授权手机号才能匹配您的服务档案', icon: 'none' });
      return;
    }
    this.doLogin(phoneCode);
  },

  async doLogin(phoneCode) {
    this.setData({ loading: true });
    wx.showLoading({ title: '登录中', mask: true });
    try {
      const { code } = await wx.login();
      const res = await api.post('/miniapp/login', { code, phoneCode });
      wx.setStorageSync('token', res.data.token);
      wx.setStorageSync('customerId', res.data.customerId);

      // 登录成功后主动引导订阅授权，避免后续收不到「服务完成」提醒
      // 用户拒绝不阻塞登录流程
      requestSubscribe(TEMPLATE_IDS, () => {
        wx.reLaunch({ url: '/pages/index/index' });
      }, () => {
        wx.reLaunch({ url: '/pages/index/index' });
      });
    } catch (err) {
      // 错误提示由 api 封装统一处理，这里只兜底 loading
    } finally {
      wx.hideLoading();
      this.setData({ loading: false });
    }
  }
});
