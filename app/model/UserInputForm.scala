package model;


case class UserInputForm(email : String,  password : String,login : String )

object  UserInputForm{
  def apply (email : String,  password : String): UserInputForm = UserInputForm(email : String,  password : String,"")
  def unapplyShort( e : UserInputForm ) = Some(e.email,e.password)
}