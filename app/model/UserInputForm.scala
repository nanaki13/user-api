package model;


case class UserInputForm(email : String, pseudonym : String, password : String )

object  UserInputForm{
  def apply (email : String,  password : String): UserInputForm = UserInputForm(email , "", password)
  def apply (email : String,  password : Option[String]): UserInputForm = UserInputForm(email , "", password.get)
  def apply (email : String,pseudonym : String,  password : Option[String]): UserInputForm = UserInputForm(email , pseudonym, password.get)
  def applyMailLogin (email : String,  login : String): UserInputForm = UserInputForm(email , login, "")
  def unapplyOptionPass (e : UserInputForm)= Some(e.email,e.pseudonym,Some(e.password))
  def mailPasswordOption(e : UserInputForm ) = Some(e.email,e.password)
}

case class UserUpdateForm(id : Int,email : Option[String],pseudonym : Option[String],  password : Option[String] )