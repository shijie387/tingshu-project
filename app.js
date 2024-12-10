
  let { updateUserInfo } = hooks_useUpdateUserInfo.useUpdateUserInfo();
  updateUserInfo();
  return {
    app
  };
}
createApp().app.mount("#app");
exports.createApp = createApp;
