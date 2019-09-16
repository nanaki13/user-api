package controller

import com.mohiva.play.silhouette.api.Silhouette
import javax.inject.Inject
import module.JWTEnv
import play.api.mvc.{BaseController, ControllerComponents}

class UserBaseController @Inject()(pcc: UserControllerComponents)
  extends BaseController
   // with RequestMarkerContext
{
  override protected def controllerComponents: ControllerComponents = pcc

  def silhouette: Silhouette[JWTEnv] = pcc.silhouette

  def userAction: UserActionBuilder = pcc.actionBuilder


}
