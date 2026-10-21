const api = require('../../utils/api');

const MAX_TAGS = 5;
const SCORE_TEXT = ['', '很不满意', '不满意', '一般', '满意', '非常满意'];

/** 目录加载后缓存，避免每次 onShow 都请求一次 */
let catalogCache = null;

Page({
  data: {
    id: null,
    score: 0,
    scoreText: '',
    groups: [],
    selected: [],
    comment: '',
    maxTags: MAX_TAGS,
    submitting: false
  },

  onLoad(options) {
    this.id = options.id;
    wx.setNavigationBarTitle({ title: '服务评价' });
  },

  onShow() {
    // 目录由后端维护（长护险服务项目目录），前端不硬编码
    if (catalogCache) {
      this.setData({ groups: catalogCache });
      return;
    }
    this.loadItems();
  },

  async loadItems() {
    try {
      const res = await api.get('/eval-items');
      catalogCache = res.data || [];
      this.setData({ groups: catalogCache });
    } catch (err) {
      // 错误提示由 api 封装处理
    }
  },

  setScore(e) {
    const score = Number(e.currentTarget.dataset.v);
    this.setData({ score, scoreText: SCORE_TEXT[score] || '' });
  },

  toggleTag(e) {
    const t = e.currentTarget.dataset.t;
    const selected = this.data.selected.slice();
    const i = selected.indexOf(t);
    if (i >= 0) {
      selected.splice(i, 1);
    } else {
      if (selected.length >= MAX_TAGS) {
        wx.showToast({ title: '最多勾选 ' + MAX_TAGS + ' 项服务项目', icon: 'none' });
        return;
      }
      selected.push(t);
    }
    this.setData({ selected });
  },

  onComment(e) {
    this.setData({ comment: e.detail.value });
  },

  async submit() {
    if (this.data.submitting) return;
    if (!this.data.score) {
      wx.showToast({ title: '请先为本次服务打分', icon: 'none' });
      return;
    }
    this.setData({ submitting: true });
    wx.showLoading({ title: '提交中', mask: true });
    try {
      await api.post('/miniapp/schedules/' + this.id + '/evaluation', {
        score: this.data.score,
        tags: this.data.selected,
        comment: this.data.comment
      });
      wx.hideLoading();
      wx.showToast({ title: '评价提交成功', icon: 'success' });
      setTimeout(() => wx.reLaunch({ url: '/pages/index/index' }), 800);
    } catch (err) {
      wx.hideLoading();
      // 错误提示由 api 封装处理
    } finally {
      this.setData({ submitting: false });
    }
  }
});
