import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { environment } from '@environments/environment';
import { User } from '@app/_models';

@Injectable({ providedIn: 'root' })
export class AuthenticationService {
    private currentTokenSubject: BehaviorSubject<any>;
    public currentToken: Observable<any>;

    private token = 'com.org.apitoken';
    constructor(private http: HttpClient) {
        this.currentTokenSubject = new BehaviorSubject<any>(JSON.parse(localStorage.getItem('com.org.apitoken')));
        this.currentToken = this.currentTokenSubject.asObservable();
    }

    public get currentTokenValue(): any {
        return this.currentTokenSubject.value;
    }

    login(email: string, password: string) {
        return this.http.post<any>(`${environment.apiUrl}/user/signIn`, { email, password })
            .pipe(map(tok => {
                // store user details and jwt token in local storage to keep user logged in between page refreshes
                localStorage.setItem('apitoken', JSON.stringify(tok));
                this.currentTokenSubject.next(tok);
                return tok;
            }));
    }

    logout() {
        // remove user from local storage to log user out
        localStorage.removeItem('apitoken');
        this.currentTokenSubject.next(null);
    }


     signUp(email: string, password: string) {
            return this.http.post<any>(`${environment.apiUrl}/user/signUp`, { "email" :  email,"password" : password,"pseudonym" : email })
              ;
        }

}
