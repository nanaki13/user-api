import { Component } from '@angular/core';
import { first } from 'rxjs/operators';

import { User } from '@app/_models';
import { UserService, AuthenticationService } from '@app/_services';

@Component({ templateUrl: 'home.component.html' })
export class HomeComponent {
    loading = false;
    users: User[];

    constructor(private userService: UserService,
              private authService: AuthenticationService) { }

    ngOnInit() {
        this.loading = true;
        this.authService.currentToken.subscribe( user => {
            if(user.isAdmin){
                this.userService.getAll().pipe(first()).subscribe(users => {
                  this.loading = false;
                  this.users = users;
              });
            }else{
              this.users = [user]
            }
        })

    }
}
