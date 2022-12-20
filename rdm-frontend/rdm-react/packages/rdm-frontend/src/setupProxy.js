const proxy = require('http-proxy-middleware');

module.exports = function(app) {
    app.use(proxy('/n2o',
        {
            target: "http://172.16.1.68:8080/#/",
            changeOrigin: true
        }
    ));
};
