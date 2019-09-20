

export class User {
    constructor(
   public id: number,
   public username: string,
   public pseudonym: string,
   public  email: string,
   public  password: string,
   public  firstName: string,
   public lastName: string,
   public  role: any,
   public  token: string) {}
    static   fromAny(params: any) {
      if(params){
        return new User(
          params.id && params.id,
          params.username && params.username,
          params.pseudonym && params.pseudonym,
          params.email && params.email,
          params.password && params.password,
          params.firstName && params.firstName,
          params.lastName && params.lastName,
          params.role && params.role,
      params.token);
      }else{
        return new User(
          1,
         '' ,
         'Anonyme',
         '',
         '',
         '',
         '',
         '',
      '');
      }


    }

    isAdmin() {
       return this.role.role === 'admin';
    }
  }

