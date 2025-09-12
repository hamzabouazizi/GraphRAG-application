const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function (app) {
    app.use(
        ['/login', '/signup', '/profile', '/health', '/actuator/health'],
        createProxyMiddleware({
            target: 'http://user-management:8080',
            changeOrigin: true,
            secure: false,
            logLevel: 'debug',
        })
    );

    app.use(
        ['/chat', '/stream'],
        createProxyMiddleware({
            target: 'http://chat-service:8001',
            changeOrigin: true,
            secure: false,
            logLevel: 'debug',
        })
    );

    app.use(
        ['/upload-pdf'],
        createProxyMiddleware({
            target: 'http://pdf-graphrag-service:8000',
            changeOrigin: true,
            secure: false,
            logLevel: 'debug',
        })
    );
};
