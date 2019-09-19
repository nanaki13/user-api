package controller

import com.mohiva.play.silhouette.api.Silhouette
import javax.inject.Inject
import module.JWTEnv
import net.logstash.logback.marker.LogstashMarker
import play.api.MarkerContext
import play.api.mvc.{BaseController, ControllerComponents, RequestHeader}

class UserBaseController @Inject()(pcc: UserControllerComponents)
  extends BaseController
    with RequestMarkerContext
{
  override protected def controllerComponents: ControllerComponents = pcc

  def silhouette: Silhouette[JWTEnv] = pcc.silhouette

  def userAction: UserActionBuilder = pcc.actionBuilder


}

/**
  * Provides an implicit marker that will show the request in all logger statements.
  */
trait RequestMarkerContext {
  import net.logstash.logback.marker.Markers

  private def marker(tuple: (String, Any)) = Markers.append(tuple._1, tuple._2)

  private implicit class RichLogstashMarker(marker1: LogstashMarker) {
    def &&(marker2: LogstashMarker): LogstashMarker = marker1.and(marker2)
  }

  implicit def requestHeaderToMarkerContext(
                                             implicit request: RequestHeader): MarkerContext = {
    MarkerContext {
      marker("id" -> request.id) && marker("host" -> request.host) && marker(
        "remoteAddress" -> request.remoteAddress)
    }
  }

}
