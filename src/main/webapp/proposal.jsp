<%@ page language="java" contentType="text/html; charset=UTF-8"
pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="sec"
           uri="http://www.springframework.org/security/tags"%>
+<%@ taglib prefix="user" tagdir="/WEB-INF/tags/user"%>

<!doctype html>
<!--[if IE 8 ]><html class="ie ie8" lang="en"> <![endif]-->
<!--[if (gte IE 9)|!(IE)]><html lang="en" class="no-js"> <![endif]-->
<html lang="en">
<head>
  
  <!-- Basic -->
  <title>Submit a proposal</title>
  
  <!-- Define Charset -->
  <meta charset="utf-8">
  
  <!-- Responsive Metatag -->
  <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
  
  <!-- Page Description and Author -->
  <meta name="description" content="Margo - Responsive HTML5 Template">
  <meta name="author" content="ZoOm Arts">
    <user:pageJavaScriptAndCss/>

</head>
<body>

	<!-- Container -->
	<div id="container">

        <user:header/>


<%--
<!-- Start Page Banner -->
<div class="page-banner">
 <div class="container">
  <div class="row">
   <div class="col-md-6">
    <h2>Blog</h2>
    <p>Blog Subtitle</p>
  </div>
  <div class="col-md-6">
    <ul class="breadcrumbs">
     <li><a href="#">Home</a></li>
     <li><a href="#">Blog</a></li>
     <li>Gallery Post With Nice Lightbox</li>
   </ul>
 </div>
</div>
</div>
</div>
<!-- End Page Banner -->
 --%>



<!-- Start Content -->
<div id="content">
 <div class="container">
  <div class="row blog-post-page">
   <div class="col-md-9 blog-box">

    <!-- Start Single Post Area -->
    <div class="blog-post gallery-post">
	
	<%--
      <!-- Start Single Post (Gallery Slider) -->
      <div class="post-head">
       <div class="touch-slider post-slider">
        <div class="item">
          <a class="lightbox" title="This is an image title" href="images/blog-02.jpg" data-lightbox-gallery="gallery1">
            <div class="thumb-overlay"><i class="fa fa-arrows-alt"></i></div>
            <img alt="" src="images/blog-02.jpg">
          </a>
        </div>
        <div class="item">
          <a class="lightbox" title="This is an image title" href="images/blog-03.jpg" data-lightbox-gallery="gallery1">
            <div class="thumb-overlay"><i class="fa fa-arrows-alt"></i></div>
            <img alt="" src="images/blog-03.jpg">
          </a>
        </div>
        <div class="item">
          <a class="lightbox" title="This is an image title" href="images/blog-04.jpg" data-lightbox-gallery="gallery1">
            <div class="thumb-overlay"><i class="fa fa-arrows-alt"></i></div>
            <img alt="" src="images/blog-04.jpg">
          </a>
        </div>
      </div>
    </div>
    <!-- End Single Post (Gallery) -->
     --%>

    <!-- Start Single Post Content -->
    <div class="post-content">
     <h2>Submit your poposal</h2>

        <form:form commandName="submission" method="post"
                   action="/cfp" enctype="multipart/form-data">
            <fieldset>
                <legend>Add submission</legend>
                <p>
                    <form:errors />
                </p>
                <dl>
                    <dt>
                        <label for="title">Title</label>
                    </dt>
                    <dd>
                        <form:input path="title" />
                    </dd>
                </dl>
                <dl>
                    <dt>
                        <label for="description">Abstract</label>
                    </dt>
                    <dd>
                        <form:textarea path="description"  cols="70" rows="5"/>
                    </dd>
                </dl>
                <dl>
                    <dt>
                        <label for="level">Session level</label>
                    </dt>
                    <dd>
                        <form:select path="level" items="${levels}"/>
                    </dd>
                </dl>
                <dl>
                    <dt>
                        <label for="speaker.firstName">First Name</label>
                    </dt>
                    <dd>
                        <form:input path="speaker.firstName" />
                    </dd>
                </dl>
                <dl>
                    <dt>
                        <label for="speaker.lastName">Last Name</label>
                    </dt>
                    <dd>
                        <form:input path="speaker.lastName" />
                    </dd>
                </dl>
                <dl>
                    <dt>
                        <label for="speaker.email">Email</label>
                    </dt>
                    <dd>
                        <form:input path="speaker.email" />
                    </dd>
                </dl>
                <dl>
                    <dt>
                        <label for="speaker.twitter">Twitter</label>
                    </dt>
                    <dd>
                        <form:input path="speaker.twitter" />
                    </dd>
                </dl>
                <dl>
                    <dt>
                        <label for="speaker.bio">Bio</label>
                    </dt>
                    <dd>
                        <form:textarea path="speaker.bio" cols="70" rows="5" />
                    </dd>
                </dl>
                <dl>
                    <dt>
                        <label for="file">Your photo ( preferred size 280x326 px )</label>
                    </dt>
                    <dd>
                        <input name="file" type="file" />
                    </dd>
                </dl>
                <sec:csrfInput />
                <form:hidden path="id" />
                <button type="submit">Submit</button>
            </fieldset>

        </form:form>


 </div>
 <!-- End Single Post Content -->
 
</div>
<!-- End Single Post Area -->

</div>



<user:sidebar/>

</div>

</div>
</div>
<!-- End content -->


        <user:footer/>
</div>
<!-- End Container -->

<!-- Go To Top Link -->
<a href="#" class="back-to-top"><i class="fa fa-angle-up"></i></a>

<div id="loader">
 <div class="spinner">
  <div class="dot1"></div>
  <div class="dot2"></div>
</div>
</div>



</body>
</html>