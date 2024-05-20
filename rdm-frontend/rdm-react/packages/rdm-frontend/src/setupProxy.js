const proxy = require('http-proxy-middleware');

module.exports = function(app) {
    app.use(proxy('/n2o',
        {
            target: "https://next.n2oapp.net/sandbox/view/Gq2zS/#/",
            changeOrigin: true
        }
    ));
};
