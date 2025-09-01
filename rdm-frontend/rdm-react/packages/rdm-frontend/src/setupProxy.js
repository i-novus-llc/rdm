const proxy = require('http-proxy-middleware');

module.exports = function(app) {
    app.use(proxy('/n2o',
        {
            target: "https://next-n2o.i-novus.ru/sandbox/view/trNht/",
            changeOrigin: true
        }
    ));
};
