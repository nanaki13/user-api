# User REST API



## Appendix

### Running

You need to download and install sbt for this application to run.

Once you have sbt installed, the following at the command prompt will start up Play in development mode:

```bash
sbt run
```

Play will start up on the HTTP port at <http://localhost:9000/>.   You don't need to deploy or reload anything -- changing any source code while the server is running will automatically recompile and hot-reload the application on the next HTTP request.

### run Test

see  test.controllers.UserRouterSpec for details

```bash
sbt test
```

Gatling is in dependency bu no test are implemented with it


### Usage

```routes
GET /user -> admin only : list th users
POST /user/signUp {email="tot@tot",password="bruce",login="bruce"}-> create an user -> return the user with id
POST /user/signIn  {email="tot@tot",password="bruce"}-> authenticate
PUT /user {id,=12, email="tot@tot",login="bruce"}-> modify an user -> return the user
DELETE /user/12 -> delete an user
```

