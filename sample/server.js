// You'll see the client-side's output on the console when you run it.

var restify = require('restify');
var errs = require('restify-errors');

// State
var next_user_id = 1;
var users = [{name: 'admin', id: 0}];

// Server
var server = restify.createServer({
    name: 'SampleAPI',
    version: '0.0.1'
});

server.use(restify.plugins.acceptParser(server.acceptable));
server.use(restify.plugins.queryParser());
server.use(restify.plugins.bodyParser());

server.get("/users", function (req, res, next) {
    res.writeHead(200, {'Content-Type': 'application/json; charset=utf-8'});
    res.end(JSON.stringify(users));
    return next();
});

server.get('/users/:id', function (req, res, next) {
    res.writeHead(200, {'Content-Type': 'application/json; charset=utf-8'});
    res.end(JSON.stringify(users[parseInt(req.params.id)]));
    return next();
});

server.post('/users', function (req, res, next) {
    var user = req.body
    for(var x in req.params) {
        user[x] = req.params[x];
    }
    user.id = next_user_id++;
    users[user.id] = user;
    res.writeHead(200, {'Content-Type': 'application/json; charset=utf-8'});
    res.end(JSON.stringify(user));
    return next();
});

server.get('/users/:id/profile_image', function (req, res, next) {
    res.writeHead(200, {'Content-Type': 'image/png'});
    res.end(users[parseInt(req.params.id)].profile_image);
    return next();
});


server.put('/users/:id/profile_image', function (req, res, next) {
    var user = users[parseInt(req.params.id)];
    var image = req.params;
    user.image = image;
    res.writeHead(200, {'Content-Type': 'application/json; charset=utf-8'});
    res.end(JSON.stringify(user));
    return next();
});


server.put('/users/:id/params_phone', function (req, res, next) {
    console.log('params_phone %s', req.params);
    var user = users[parseInt(req.params.id)];
    if (user == undefined) {
        return next(new errs.InvalidArgumentError("no such id"));
    }
    user.phone = req.params.phone;
    res.writeHead(200, {'Content-Type': 'application/json; charset=utf-8'});
    res.end(JSON.stringify(user));
    return next();
});

server.put('/users/:id', function (req, res, next) {
    var user = users[parseInt(req.params.id)];
    if (user == undefined) {
        return next(new errs.InvalidArgumentError("no such id"));
    }
    for(var x in req.body) {
        user[x] = req.body[x];
    }
    var changes = req.params;
    delete changes.id;
    for(var x in changes) {
        user[x] = changes[x];
    }
    res.writeHead(200, {'Content-Type': 'application/json; charset=utf-8'});
    res.end(JSON.stringify(user));
    return next();
});

server.del('/users/:id', function (req, res, next) {
    var user = users[parseInt(req.params.id)];
    if (user == undefined) {
        return next(new errs.InvalidArgumentError("no such id"));
    }
    delete users[parseInt(req.params.id)];
    res.writeHead(200, {'Content-Type': 'application/json; charset=utf-8'});
    res.end(JSON.stringify(true));
    return next();
});

server.listen(9395, function () {
    console.log('%s listening at %s', server.name, server.url);
});

