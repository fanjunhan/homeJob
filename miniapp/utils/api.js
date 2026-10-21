/**
 * 网络请求封装：统一携带 JWT、处理 401 与业务错误码。
 *
 * 注意：getApp() 不能在模块顶层调用。模块加载时 App() 注册可能尚未完成，
 * 顶层取值会拿到 undefined，后续 app.globalData.baseUrl 直接崩溃。
 * 因此改为请求时惰性获取。
 */
const DEFAULT_BASE_URL = 'https://api.service-cloud.example.com';

function app() {
  return getApp({ allowDefault: true }) || {};
}

function baseUrl() {
  const a = app();
  return (a.globalData && a.globalData.baseUrl) || DEFAULT_BASE_URL;
}

function request(options) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token') || '';
    wx.request({
      url: baseUrl() + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      timeout: options.timeout || 15000,
      header: {
        'content-type': 'application/json',
        'Authorization': token ? 'Bearer ' + token : ''
      },
      success(res) {
        if (res.statusCode === 401) {
          wx.removeStorageSync('token');
          wx.reLaunch({ url: '/pages/login/login' });
          reject(res.data);
          return;
        }
        if (res.statusCode >= 500) {
          wx.showToast({ title: '服务暂时不可用，请稍后重试', icon: 'none' });
          reject(res.data);
          return;
        }
        // 统一响应体：{ code: 0, data, message }
        if (res.data && res.data.code === 0) {
          resolve(res.data);
        } else {
          wx.showToast({ title: (res.data && res.data.message) || '请求失败', icon: 'none' });
          reject(res.data);
        }
      },
      fail(err) {
        // 用户取消/网络中断时不打扰
        if (err && err.errMsg && err.errMsg.indexOf('cancel') >= 0) {
          reject(err);
          return;
        }
        wx.showToast({ title: '网络异常，请检查网络后重试', icon: 'none' });
        reject(err);
      }
    });
  });
}

module.exports = {
  get: (url, data) => request({ url, method: 'GET', data }),
  post: (url, data) => request({ url, method: 'POST', data })
};
