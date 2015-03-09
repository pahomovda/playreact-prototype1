package controllers

import javax.script.ScriptEngineManager
import play.api.Play.current
import play.api._
import play.api.cache.Cached
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import play.twirl.api.Html
import scala.concurrent.Promise

object Application extends Controller {



  def requireJsConfig = Cached("require_js_config") {
    Action {
      Ok(views.html.requireJsConfig()).as("application/javascript")
    }
  }

  def serverSideWithJsEngine(uri: String) = Action.async { request =>

    // TODO - move this block to constructor in production
    val manager: ScriptEngineManager = new ScriptEngineManager(null)
    val engine = manager.getEngineByName("nashorn")
    val invocable = engine.asInstanceOf[javax.script.Invocable]
    val nashornpolyfill = scala.io.Source.fromFile(Play.getFile("public/nashorn-polyfill.js")).mkString
    val serversidejs = scala.io.Source.fromFile(Play.getFile("public/serverside.js")).mkString
    engine.eval(nashornpolyfill)
    engine.eval(serversidejs)
    val markupPromise = Promise[String]()
    invocable.invokeFunction("prerender", request.uri, markupPromise)

    for {
      markup <- markupPromise.future
    } yield {
      Ok(views.html.main(Html(markup)))
    }
  }

}