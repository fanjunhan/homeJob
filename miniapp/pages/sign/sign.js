const api = require('../../utils/api');

Page({
  data: {
    id: null,
    submitting: false,
    hasSig: false
  },

  onLoad(options) {
    this.id = options.id;
    wx.setNavigationBarTitle({ title: '电子签名' });
  },

  onReady() {
    // canvas 2d 接口（基础库 2.9.0+）
    wx.createSelectorQuery()
      .select('#sigCanvas')
      .fields({ node: true, size: true })
      .exec((res) => {
        if (!res[0] || !res[0].node) {
          wx.showToast({ title: '签名区域初始化失败，请重进页面', icon: 'none' });
          return;
        }
        const canvas = res[0].node;
        const ctx = canvas.getContext('2d');

        // dpr 适配：canvas 位图按物理像素放大，绘制坐标用 CSS 像素
        // getSystemInfoSync 已废弃，改用 getWindowInfo（基础库 2.20.1+）
        let pixelRatio = 2;
        try {
          if (wx.getWindowInfo) {
            pixelRatio = wx.getWindowInfo().pixelRatio || 2;
          } else {
            pixelRatio = wx.getSystemInfoSync().pixelRatio || 2;
          }
        } catch (e) {
          // 降级用固定值，保证可用
        }

        canvas.width = res[0].width * pixelRatio;
        canvas.height = res[0].height * pixelRatio;
        ctx.scale(pixelRatio, pixelRatio);

        ctx.lineWidth = 2.5;
        ctx.lineCap = 'round';
        ctx.lineJoin = 'round';
        ctx.strokeStyle = '#1F2329';

        this.canvas = canvas;
        this.ctx = ctx;
        // 记住 CSS 尺寸与缩放比：ctx 已 scale，clearRect 需要用 CSS 像素坐标
        this.cssW = res[0].width;
        this.cssH = res[0].height;
        this.hasSig = false;
      });
  },

  onCanvasError() {
    wx.showToast({ title: '当前设备不支持手写签名', icon: 'none' });
  },

  onTouchStart(e) {
    if (!this.ctx) return;
    const p = e.touches[0];
    this.ctx.beginPath();
    this.ctx.moveTo(p.x, p.y);
  },

  onTouchMove(e) {
    if (!this.ctx) return;
    const p = e.touches[0];
    this.ctx.lineTo(p.x, p.y);
    this.ctx.stroke();
    if (!this.hasSig) {
      this.hasSig = true;
      this.setData({ hasSig: true });
    }
  },

  clearCanvas() {
    if (!this.ctx) return;
    // ctx 已按 dpr scale，clearRect 传 CSS 尺寸即可覆盖整块画布
    this.ctx.clearRect(0, 0, this.cssW || this.canvas.width, this.cssH || this.canvas.height);
    this.hasSig = false;
    this.setData({ hasSig: false });
  },

  async submit() {
    if (!this.hasSig) {
      wx.showToast({ title: '请先手写签名', icon: 'none' });
      return;
    }
    if (this.data.submitting) return;
    this.setData({ submitting: true });
    wx.showLoading({ title: '提交中', mask: true });
    try {
      // 1. canvas 导出临时图片
      const tempPath = await new Promise((resolve, reject) => {
        wx.canvasToTempFilePath({
          canvas: this.canvas,
          fileType: 'png',
          success: (res) => resolve(res.tempFilePath),
          fail: reject
        });
      });
      // 2. 读为 base64 dataURL，由后端落 OSS。
      //    与后端契约保持一致（后端 OssStorageService.uploadBase64 接收 dataURL）；
      //    签名图约百 KB，base64 后仍在 JSON 请求合理范围内。
      const base64 = wx.getFileSystemManager().readFileSync(tempPath, 'base64');
      const dataUrl = 'data:image/png;base64,' + base64;
      await api.post('/miniapp/schedules/' + this.id + '/signature', { image: dataUrl });
      wx.hideLoading();
      wx.showToast({ title: '签名成功', icon: 'success' });
      setTimeout(() => wx.navigateTo({ url: '/pages/rate/rate?id=' + this.id }), 800);
    } catch (err) {
      wx.hideLoading();
      // 错误提示由 api 封装处理
    } finally {
      this.setData({ submitting: false });
    }
  }
});
