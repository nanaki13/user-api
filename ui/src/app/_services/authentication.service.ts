import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { environment } from '@environments/environment';
import { User } from '@app/_models';
import { UserService } from './user.service';

const token = 'com.org.apitoken';
@Injectable({ providedIn: 'root' })
export class AuthenticationService {
    private currentTokenSubject: BehaviorSubject<any>;
    public currentToken: Observable<any>;
    private currentUserSubject: BehaviorSubject<User>;
    public currentUser: Observable<User>;


    constructor(private http: HttpClient,private userServive : UserService) {
        this.currentTokenSubject = new BehaviorSubject<any>(JSON.parse(localStorage.getItem(token)));
        this.currentToken = this.currentTokenSubject.asObservable();
        this.currentUserSubject = new BehaviorSubject<User>(User.fromAny(JSON.parse(localStorage.getItem('com.org.jo.user'))));
        this.currentUser = this.currentUserSubject.asObservable();
    }

    public get currentTokenValue(): any {
        return this.currentTokenSubject.value;
    }

    login(email: string ,pseudonym: string, password: string) {
        return this.http.post<any>(`${environment.apiUrl}/user/signIn`, { email, password, pseudonym })
            .pipe(map(tok => {
                // store user details and jwt token in local storage to keep user logged in between page refreshes

                localStorage.setItem('com.org.jo.apitoken', JSON.stringify(tok));
                this.currentTokenSubject.next(tok);
                return tok;
            })).pipe(map(tok => {
              // store user details and jwt token in local storage to keep user logged in between page refreshes
              this.userServive.getSelf().subscribe( u => {
                this.currentUserSubject.next(User.fromAny(u));
                localStorage.setItem('com.org.jo.user', JSON.stringify(u));
              })
              return tok;
          }))
    }

    logout() {
        // remove user from local storage to log user out
        localStorage.removeItem('com.org.jo.apitoken');
        localStorage.removeItem('com.org.jo.user');
        this.currentTokenSubject.next(null);
    }


     signUp(email: string ,pseudonym: string, password: string) {
            return this.http.post<any>(`${environment.apiUrl}/user/signUp`, { email ,pseudonym, password })
              ;
        }

}
