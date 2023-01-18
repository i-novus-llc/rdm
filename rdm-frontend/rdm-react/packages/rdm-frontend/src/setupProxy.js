const proxy = require('http-proxy-middleware');

module.exports = function(app) {
    app.use(proxy('/n2o',
        {
            target: "https://cloud.develop.i-novus.ru/admin/#/rdm",
            changeOrigin: true
        }
    ));
};
