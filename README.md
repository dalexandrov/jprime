# jprime

TODO
1)currently in the SecurityConfig this two lines are commented.. but should not BE

  //.antMatchers("/admin/**").hasRole("ADMIN") // #6
           //.anyRequest().authenticated() // 7

ALSO a user manager should be implemented to get users from the database
Instead of the hardcoded .inMemoryAuthentication()
                .withUser("user").password("password").roles("USER")
                .and()
                .withUser("admin").password("password").roles("ADMIN","USER");


2) login form should be implemented ... (registration if needed)
3) User Entity probably should be updated.
4) userFacade is not implemented fully.. there should have a method for :
display all tags.
display all articles in a tag. 
IndexController should use this userFacade to set all sponsors/speakers/etc that are displayed on the homepage (currently as MOCK)
5) NavController should get the articles in one tag (passed by param) and set this in the model + redirect to a jsp page
that looks like blog.html 
6) NavController should have one more method to display a single article (based on name)
7)CFP form
8) payment provider
