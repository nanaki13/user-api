export class User {
    id: number;
    username: string;
    pseudonym: string;
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    role: any;
    token?: string;

    isAdmin() {
       return this.role.role === "admin"
    }
}
