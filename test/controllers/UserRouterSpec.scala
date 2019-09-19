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
    Json.toJson(Json.obj(("email", e.email), ("login", e.login)))
  }
  val writerEmailLoginPassowrd: Writes[UserInputForm] = (e: UserInputForm) => {
    Json.toJson(Json.obj(("email", e.email), ("login", e.login), ("password", e.password)))
  }
  val writeForUpdate: Writes[UserUpdateForm] = (e: UserUpdateForm) => {
    Json.toJson(Json.obj(("id", e.id),("email", e.email), ("login", e.login), ("password", e.password)))
  }
  var token : Option[String]= None
  var id : Int = -1
  "UserRouter" should {


    "check sign In" in {
      val request = FakeRequest(POST, "/user/signUp").withHeaders(("Content-Type" , "application/json"))
      val resp: Future[Result] = route(app, request, Json.toJson(UserInputForm("test.test@test.com", "foo", "pass"))(writerEmailLoginPassowrd)).get
      status(resp) mustBe 200
      val user: User = Json.fromJson[User](contentAsJson(resp)).get
      user.login mustBe "foo"
      id= user.id
      val request2 = FakeRequest(POST, "/user/signIn").withHeaders(("Content-Type" , "application/json"))
      val resp2: Future[Result] = route(app, request2, Json.toJson(UserInputForm("test.test@test.com", "foo", "pass"))(writerEmailPassword)).get
      status(resp2) mustBe 200
      token = Some(headers(resp).get("X-Auth-Token")).get

    }
    "check sign Up" in {
      val request = FakeRequest(POST, "/user/signIn").withCSRFToken
      val resp: Future[Result] = route(app, request, Json.toJson(UserInputForm("test.test@test.com", "pass"))(writerEmailPassword)).get
      status(resp) mustBe 200
       token = Some(headers(resp).get("X-Auth-Token")).get

    }
    "check update" in {
      val request = FakeRequest(PUT, "/user")
        .withHeaders(HOST -> "localhost:9000", "X-Auth-Token" -> token.get)
        .withCSRFToken
      val resp: Future[Result] = route(app, request, Json.toJson(UserUpdateForm(id,Some("test.test@test.com"),Some("bar"), None))(writeForUpdate)).get
      val user: User = Json.fromJson[User](contentAsJson(resp)).get
      user.email mustBe "test.test@test.com"
      user.login mustBe "bar"
    }
  }
}

