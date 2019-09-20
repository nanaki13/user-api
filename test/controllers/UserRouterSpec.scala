import model.{User, UserInputForm, UserUpdateForm}
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.libs.json.{Json, Writes}
import play.api.mvc.Result
import play.api.test.CSRFTokenHelper._
import play.api.test.Helpers._
import play.api.test._

import scala.concurrent.Future

class UserRouterSpec extends PlaySpec with GuiceOneAppPerSuite with RouteInvokers with play.api.test.Writeables {

  val writerEmailPassword: Writes[UserInputForm] = (e: UserInputForm) => {
    Json.toJson(Json.obj(("email", e.email), ("password", e.password)))
  }
  val writerEmailLogin: Writes[UserInputForm] = (e: UserInputForm) => {
    Json.toJson(Json.obj(("email", e.email), ("pseudonym", e.pseudonym)))
  }
  val writerEmailLoginPassowrd: Writes[UserInputForm] = (e: UserInputForm) => {
    Json.toJson(Json.obj(("email", e.email), ("pseudonym", e.pseudonym), ("password", e.password)))
  }
  val writeForUpdate: Writes[UserUpdateForm] = (e: UserUpdateForm) => {
    Json.toJson(Json.obj(("id", e.id),("email", e.email), ("pseudonym", e.pseudonym), ("password", e.password)))
  }
  var token : Option[String]= None
  var userId : Int = -1
  "UserRouter" should {


    "check sign In" in {
      val request = FakeRequest(POST, "/user/signUp").withHeaders(("Content-Type" , "application/json"))
      val resp: Future[Result] = route(app, request, Json.toJson(UserInputForm("test.test@test.com", "foo", "pass"))(writerEmailLoginPassowrd)).get

      val cntString: String = contentAsString(resp)
      println(cntString)
      val user: User = Json.fromJson[User](contentAsJson(resp)).get
      user.pseudonym mustBe "foo"
      userId= user.id
      status(resp) mustBe OK

    }
    "check sign Up" in {
      val request = FakeRequest(POST, "/user/signIn").withCSRFToken
      val resp: Future[Result] = route(app, request, Json.toJson(UserInputForm("test.test@test.com", "pass"))(writerEmailPassword)).get
      status(resp) mustBe OK
      token = Some(headers(resp).get("X-Auth-Token")).get

    }
    "test 401 on get list" in {
      val request = FakeRequest(GET, "/user")
        .withHeaders(HOST -> "localhost:9000", "X-Auth-Token" -> token.get)
        .withCSRFToken
      val resp: Future[Result] = route(app, request,"").get
      status(resp) mustBe UNAUTHORIZED

    }
    "check update" in {
      val request = FakeRequest(PUT, "/user")
        .withHeaders(HOST -> "localhost:9000", "X-Auth-Token" -> token.get)
        .withCSRFToken
      val resp: Future[Result] = route(app, request, Json.toJson(UserUpdateForm(userId,Some("test.test@test.com"),Some("bar"), None))(writeForUpdate)).get
      val user: User = Json.fromJson[User](contentAsJson(resp)).get
      user.email mustBe "test.test@test.com"
      user.pseudonym mustBe "bar"
    }

    "test admin" in {
      val email = app.configuration.get[String]("application.admin.email")
      val password = app.configuration.get[String]("application.admin.password")
      val request = FakeRequest(POST, "/user/signIn").withCSRFToken
      val resp: Future[Result] = route(app, request, Json.toJson(UserInputForm(email, password))(writerEmailPassword)).get
      status(resp) mustBe 200
      token = Some(headers(resp).get("X-Auth-Token")).get

    }

    "test admin get list" in {
      val request = FakeRequest(GET, "/user")
        .withHeaders(HOST -> "localhost:9000", "X-Auth-Token" -> token.get)
        .withCSRFToken
      val resp: Future[Result] = route(app, request,"").get
      status(resp) mustBe OK
    }

    "test admin delete" in {
      val request = FakeRequest(DELETE, s"/user/${userId}")
        .withHeaders(HOST -> "localhost:9000", "X-Auth-Token" -> token.get)
        .withCSRFToken
      val resp: Future[Result] = route(app, request,"").get
      status(resp) mustBe OK
    }
  }
}

