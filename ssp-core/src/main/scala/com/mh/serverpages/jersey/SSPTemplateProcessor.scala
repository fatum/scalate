package com.mh.serverpages.jersey

import java.io.{OutputStream}
import java.net.MalformedURLException
import javax.servlet.{ServletContext}
import com.sun.jersey.server.impl.container.servlet.RequestDispatcherWrapper
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import com.sun.jersey.api.core.{HttpContext, ResourceConfig}
//import org.apache.log4j.Logger
import com.sun.jersey.api.container.ContainerException
import com.sun.jersey.spi.template.TemplateProcessor
import javax.ws.rs.core.Context

/**
 * @version $Revision : 1.1 $
 */
class SSPTemplateProcessor(@Context resourceConfig: ResourceConfig) extends TemplateProcessor {
  @Context
  var servletContext: ServletContext = _
  @Context
  var hc: HttpContext = _
  @Context
  var request: HttpServletRequest = _
  @Context
  var response: HttpServletResponse = _

  val basePath = resourceConfig.getProperties().get("org.scala-tools.serverpages.config.property.SSPTemplatesBasePath") match {
    case path: String =>
      if (path(0) == '/') {
        path
      }
      else {
        "/" + path
      }
    case _ => ""
  }


  def resolve(requestPath: String): String = {
    if (servletContext == null) {
      println("No servlet context")
      return null
    }

    try {
      val path = if (basePath.length > 0) {basePath + requestPath} else {requestPath}

      // TODO this code actually results in looking up the resource twice
      // once here first then again Lift land
      // I wonder if there's a better way to do this just once?
      if (servletContext.getResource(path) == null) {
        val sspPath = path + ".ssp"
        if (servletContext.getResource(sspPath) == null) {
          println("WARN: No template found for path '" + path + "' or '" + sspPath + "'")
          null
        }
        else {
          println("Found template for path '" + sspPath + "'")
          sspPath
        }
      }
      else {
        println("Found template for path '" + path + "'")
        path
      }
    } catch {
      case e: MalformedURLException =>
      // TODO log
    }
    null
    // println("No template found for " + path)
  }

  def writeTo(resolvedPath: String, model: AnyRef, out: OutputStream): Unit = {
    out.flush();

    println("Writing from dispatcher: " + resolvedPath)

    val dispatcher = servletContext.getRequestDispatcher(resolvedPath);
    if (dispatcher == null) {
      throw new ContainerException("No request dispatcher for: " + resolvedPath);
    }

    val wrapper = new RequestDispatcherWrapper(dispatcher, basePath, hc, model);
    try {
      println("Have dispatcher: " + wrapper)
      wrapper.forward(request, response);
      //wrapper.forward(requestInvoker.get(), responseInvoker.get());
    } catch {
      case e: Exception => throw new ContainerException(e);
    }
  }

}
