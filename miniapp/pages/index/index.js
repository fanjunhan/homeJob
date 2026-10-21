const api = require('../../utils/api');
const status = require('../../utils/status');
const subscribe = require('../../utils/subscribe');

Page({
  data: {
    pending: [],
    history: [],
    loading: true,
    subscribed: false
  },

  onShow() {
    this.load();
    this.setData({ subscribed: subscribe.hasAnyAuth(subscribe.TEMPLATE_IDS) });
  },

  onPullDownRefresh() {
    this.load().then(() => wx.stopPullDownRefresh());
  },

  async load() {
    this.setData({ loading: true });
    try {
      const res = await api.get('/miniapp/my-schedules');
      const all = status.decorate(res.data || []);
      // 待确认：员工已完成打卡，等待客户签名/评价
      this.setData({
        pending: all.filter((s) => status.isPending(s.status)),
        history: all.filter((s) => !status.isPending(s.status))
      });
    } catch (err) {
      // 错误提示由 api 封装处理
    } finally {
      this.setData({ loading: false });
    }
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/service/service?id=' + e.currentTarget.dataset.id });
  },

  /** 首页引导开启订阅，授权成功后刷新状态 */
  openSubscribe() {
    subscribe.requestSubscribe(subscribe.TEMPLATE_IDS, () => {
      this.setData({ subscribed: true });
      wx.showToast({ title: '已开启服务通知', icon: 'success' });
    }, () => {
      wx.showToast({ title: '可稍后在「我的」中开启', icon: 'none' });
    });
  }
});
