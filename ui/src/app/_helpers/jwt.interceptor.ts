import { Injectable } from '@angular/core';
import { HttpRequest, HttpHandler, HttpEvent, HttpInterceptor } from '@angular/common/http';
import { Observable } from 'rxjs';

import { AuthenticationService } from '@app/_services';

@Injectable()
export class JwtInterceptor implements HttpInterceptor {
    constructor(private authenticationService: AuthenticationService) { }

    intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        // add authorization header with jwt token if available
        let currentToken = this.authenticationService.currentTokenValue;
        if (currentToken && currentToken.token) {
            request = request.clone({
                setHeaders: {
                    'X-Auth-Token': `${currentToken.token}`
                }
            });
        }

        return next.handle(request);
    }
}
