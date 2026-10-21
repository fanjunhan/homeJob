/**
 * 小程序全局入口。
 *
 * 约定：
 *  - baseUrl 与订阅模板 ID 由后端/运营统一提供，生产环境不要在代码里写死；
 *  - token 与 customerId 走本地存储，请求头由 utils/api 统一注入。
 */
const DEFAULT_BASE_URL = 'https://api.service-cloud.example.com';

App({
  globalData: {
    baseUrl: DEFAULT_BASE_URL,
    /** 服务完成提醒订阅消息模板 ID，需在小程序后台申请 */
    templateIds: {
      serviceFinish: 'MINIAPP_TMPL_FINISH'
    }
  },

  onLaunch() {
    this.globalData.token = wx.getStorageSync('token') || '';
    this.globalData.customerId = wx.getStorageSync('customerId') || '';
  },

  /** 登录态失效时的统一出口 */
  onUnauthorized() {
    wx.removeStorageSync('token');
    wx.removeStorageSync('customerId');
    wx.reLaunch({ url: '/pages/login/login' });
  }
});
