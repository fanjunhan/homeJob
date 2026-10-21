const api = require('../../utils/api');
const status = require('../../utils/status');

Page({
  data: {
    id: null,
    s: null,
    loading: true,
    signatureUrl: '',
    signedAt: '',
    starsText: ''
  },

  onLoad(options) {
    this.id = options.id;
    wx.setNavigationBarTitle({ title: '服务确认' });
  },

  onShow() {
    this.load();
  },

  async load() {
    this.setData({ loading: true });
    try {
      const res = await api.get('/miniapp/schedules/' + this.id);
      const s = status.decorate([res.data])[0] || null;
      if (s) {
        if (!Array.isArray(s.tags)) {
          s.tags = s.tags ? String(s.tags).split(',') : [];
        }
        // 星级文本在 JS 侧算好，避免 WXML 里依赖字符串方法
        const score = Number(s.score) || 0;
        this.setData({
          starsText: '★'.repeat(score) + '☆'.repeat(Math.max(0, 5 - score))
        });
      }
      this.setData({
        s,
        signatureUrl: (s && s.signatureUrl) || '',
        signedAt: (s && s.signedAt) || '',
        loading: false
      });
    } catch (err) {
      this.setData({ loading: false, s: null });
    }
  },

  goSign() {
    wx.navigateTo({ url: '/pages/sign/sign?id=' + this.id });
  },

  goRate() {
    wx.navigateTo({ url: '/pages/rate/rate?id=' + this.id });
  },

  /** 点击签名图全屏预览 */
  previewSignature() {
    if (this.data.signatureUrl) {
      wx.previewImage({ urls: [this.data.signatureUrl] });
    }
  }
});
