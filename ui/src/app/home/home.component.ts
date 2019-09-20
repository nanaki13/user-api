import { Component, OnInit } from '@angular/core';
import { first } from 'rxjs/operators';

import { User } from '@app/_models';
import { UserService, AuthenticationService } from '@app/_services';

@Component({ templateUrl: 'home.component.html' })
export class HomeComponent implements OnInit{
    loading = false;
    users: User[];
    user: User;
    constructor(private userService: UserService,
              private authService: AuthenticationService) { }

    ngOnInit() {
        this.loading = true;

        this.authService.currentUser.subscribe( user => {
            this.user = user;
            if(user.isAdmin()){
                this.userService.getAll().pipe(first()).subscribe(users => {
                  this.loading = false;
                  this.users = users;
              });
            }else{
              this.loading = false;
              this.users = [user]
            }
        })

    }

    logout(){

      this.authService.logout();
      location.reload()
    }
}
