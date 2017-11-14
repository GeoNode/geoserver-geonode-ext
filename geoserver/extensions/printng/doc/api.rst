-----------
Printng API
-----------

There are three endpoints in the printng API:

.. list-table:: Endpoints

   * - URL (relative to /geoserver/rest/printng/ )
     - Purpose
   * - render.{ext}
     - Render the provided HTML
   * - freemarker/{template}.{ext}
     - Render the specified template
   * - freemarker/{template}
     - Add/modify the template

Render Endpoint
---------------

The render API endpoint currently takes a POST containing the raw HTML to be rendered.
The extension provided determines the output format - this can be one of:

  * pdf
  * jpg, png, gif
  * html (this only really makes sense for the freemarker endpoint)
  * json (JSON Response Type)

The `JSON Response Type` will consist of a single object with a single property `getURL` that
will point to a URL where the actual result can be obtained. The output format of the actual
product must be specified via the URL parameter `format`. For example::

  /render.json?format=pdf

The image response formats support the following URL parameters:

  * width, height - set the output width. the generated image will be scaled from it's render
    size to the specified size

To support authentication/authorization with remote URLs (for example, a WMS), the following
security parameters can be specified:

  * cookie - in the format `host,cookiename,value`
  * auth - in the format `host,user,password`

Any number of cookie or auth credentials can be supplied. For resources that match the host,
the provided credentials will be used for the request.

Document API
------------

Some information can be specified via the actual HTML document. For PDF output, the CSS3
page media `module <http://www.w3.org/TR/css3-page/#page-size>`_ is used. This supports 
all of the standardized media sizes (A3, A4, legal, etc.) and portrait or landscape.

For image output, the document is scanned for size hints. First the body element is examined
for the presence of a `width` and/or `height` style attribute. Then each body child is also
scanned for the attribute. This is the image *render* size. If the render size differs from
the specified output size, the output will be scaled.

An enhancement would be to support paged-media rules for image size.

Freemarker Render Endpoint
--------------------------

This is just like the render endpoint except that the named template will be used to generate
the html. 

@todo


Freemarker Template Endpoint
----------------------------

@todo

 
