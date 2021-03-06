package route

import controller.UserController
import javax.inject.Inject
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

class UserRouter @Inject()(userController: UserController) extends SimpleRouter{
  override def routes: Routes = {
    case GET(p"/") =>  userController.index
    case POST(p"/signUp") =>  userController.signUp
    case POST(p"/signIn") =>  userController.signIn
    case PUT(p"/") =>  userController.update
    case DELETE(p"/${id}"  ) =>  userController.delete(id.toInt)
    case GET(p"/self") =>  userController.getSelf
    case GET(p"/${id}") =>  userController.get(id.toInt)
  }
}
