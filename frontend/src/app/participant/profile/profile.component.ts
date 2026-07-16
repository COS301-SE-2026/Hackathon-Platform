import {Component} from '@angular/core';
import{CommonModule} from '@angular/common';
import{FormsModule} from '@angular/forms';
import {RouterModule} from '@angular/router';

@Component({
  selector: 'app-profile',
  imports: [CommonModule, FormsModule, RouterModule],
  standalone : true,
 templateUrl: './profile.component.html',
 styleUrl: './profile.component.scss'
})
export class ProfileComponent {
    isEditing = false;

    profile = {
        firstName: 'Jane ',   
        lastName: 'Smith',
        email: 'janesmith@example.co.za',
        username: 'janeS' , 
        currentPassword: '',
        newPassword: '',
    };      

    participationHistory = [
        {
            name: 'Entelect Challenge', team: 'ByteForce', rank: '#11', score: 122},

    
];

toggleEdit() : void {
    this.isEditing = !this.isEditing;
}


  saveProfile() : void {
    console.log('Saving profile:', this.profile);
    this.isEditing =  false;
    alert('Profile saved successfully!');

}
 cancelEdit() : void {
    this.profile = {
        firstName: 'Jane ',
        lastName: 'Smith',
        email: 'janeSmith@eample.co.za',
        username: 'janeS' ,
        currentPassword: '',
        newPassword: '',   };
    this.isEditing = false;
 }
}