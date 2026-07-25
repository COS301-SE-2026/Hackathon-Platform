import { Component, inject} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule} from '@angular/router';
import { DomSanitizer, SafeHtml} from '@angular/platform-browser';


@Component ({
    selector: 'app-brand-style-guide',
    standalone: true ,
    imports: [CommonModule, RouterModule],
    templateUrl: './brand-style-guide.component.html',
    styleUrls: ['./brand-style-guide.component.scss']
})

export class BrandStyleGuideComponent  {
    private readonly sanitizer = inject(DomSanitizer);

    colours = {
        primary: [
            {name: 'Navy Darkest', hex:'#13192D',rgb:'(19,25,45)',hsl:'hsl(228, 41%, 13%)',usage:'Backgrounds,text'},
            {name: 'Navy Deep', hex:'#062951',rgb:'rgb(6, 41, 81)',hsl:'hsl(212, 86%, 17%)',usage:'Navigation, sub-navigation'},
            {name: 'Primary Blue', hex:'#1983C2',rgb:'rgb(25, 131, 194)',hsl:'hsl(202, 77%, 43%)',usage:'Primary actions, links'},
            {name: 'Teal', hex:'#018E9A',rgb:'rgb(1, 142, 154)',hsl:'hsl(185, 99%, 30%)',usage:'Avatars, accent'},
            {name: 'Green', hex:'#268B42',rgb:'rgb(38, 139, 66)',hsl:'hsl(136, 57%, 35%)',usage:'Success, positive actions'},
            {name: 'Lime Accent', hex:'#A1BF24',rgb:'rgb(161, 191, 36)',hsl:'hsl(71, 68%, 45%)',usage:'Badges, highlights'},
        ],
        neutrals:[
            {name: 'White', hex:'#FFFFFF',rgb:'rgb(255, 255, 255)',usage:'Text on dark, cards'},
            {name: 'Off-White', hex:'#F4F6F9',rgb:'rgb(244, 246, 249)',usage:'Page backgrounds'},
            {name: 'Light Gray', hex:'#E7EAF0',rgb:'rgb(231, 234, 240)',usage:'Borders, dividers'},
            {name: 'Mid Gray', hex:'#A3AABB',rgb:'rgb(163, 170, 187)',usage:'Placeholder text'},
            {name: 'Dark Gray', hex:'#3A4357',rgb:'rgb(58, 67, 87)',usage:'Body text'},
            {name: 'Black', hex:'#13192D',rgb:'rgb(19, 25, 45)',usage:'Headings, dark backgrounds'},

        ],
        semantic: [
            {name: 'Success', hex:'#268B42',rgb:'rgb(38, 139, 66)',usage:'Success messages, completed status'},
            {name: 'Warning', hex:'#D4690F',rgb:'rgb(212, 105, 15)',usage:'Warnings, caution'},
            {name: 'Error', hex:'#C0392B',rgb:'rgb(192, 57, 43)',usage:'Errors, destructive actions'},
            {name: 'Info', hex:'#1983C2',rgb:'rgb(25, 131, 194)',usage:'Informational message'},
            

        ]
    };

    typography = {
        scale: [
        {name : 'Display', size:'3.5rem / 56px',weight:'700',lineHeight:'1.1',letterSpacing: '-0.02em', usage:'Hero sections,large titles'},
        {name : 'H1', size:'2.5rem / 40px',weight:'700',lineHeight:'1.2',letterSpacing: '-0.01em', usage:'Page headers'},
        {name : 'H2', size:'1.75rem / 28px',weight:'700',lineHeight:'1.3',letterSpacing: '-0.01em', usage:'Section headings'},
        {name : 'H3', size:'1.25rem / 20px',weight:'600',lineHeight:'1.4',letterSpacing: '0', usage:'Card titles, sub-headings'},
        {name : 'Body Large', size:'1.125rem / 18px',weight:'400',lineHeight:'1.6',letterSpacing: '0', usage:'Lead text, descriptions'},
        {name : 'Body', size:'1rem / 16px',weight:'400',lineHeight:'1.5',letterSpacing: '0', usage:'Main body text'},
        {name : 'Small', size:'0.875rem / 14px',weight:'400',lineHeight:'1.5',letterSpacing: '0.01em', usage:'Labels, meta text'},
        {name : 'Caption', size:'0.75rem / 12px',weight:'400',lineHeight:'1.5',letterSpacing: '0.02em', usage:'Captions, helper text'}

        ],
        weights: [
        {name: 'Regular', value: '400', usage:'Body text,captions'},
        {name: 'Medium', value: '500', usage:'Labels, sub-headings'},
        {name: 'Semibold', value: '600', usage:'Buttons, string emphasis'},
        {name: 'Bold', value: '700', usage:'Headings, display text'}

    ]
    };

    tokens = {
        spacing: [
        {name: 'space-0', value:'0' ,usage: 'No spacing'},
        {name: 'space-1', value:'4px' ,usage: 'Tight spacing'},
        {name: 'space-2', value:'8px' ,usage: 'Small gaps'},
        {name: 'space-3', value:'12px' ,usage: 'Medium gaps'},
        {name: 'space-4', value:'16px' ,usage: 'Standard gaps'},
        {name: 'space-6', value:'24px' ,usage: 'Large gaps'},
        {name: 'space-8', value:'32px' ,usage: 'Section gaps'},
        {name: 'space-12', value:'48px' ,usage: 'Page spacing'},
        ],
        radius: [
        {name: 'radius-sm', value:'4px' ,usage: 'Inputs, small elements'},
        {name: 'radius-md', value:'8px' ,usage: 'Buttons, cards, badges'},
        {name: 'radius-lg', value:'12px' ,usage: 'Cards, modals'},
        {name: 'radius-xl', value:'16px' ,usage: 'Modals, panels'},
        {name: 'radius-full', value:'9999px' ,usage: 'Pills, avatars'}
        ],
        shadows: [
        {name: 'shadow-sm', value:'0 1px 2px rgba(19, 25, 45, 0.04)' ,usage: 'Subtle depth'},
        {name: 'shadow-md', value:'0 1px 2px rgba(6, 41, 81, 0.15)' ,usage: 'Buttons'},
        {name: 'shadow-lg', value:'0 4px 12px rgba(19, 25, 45, 0.08)' ,usage: 'Card on hover'},
        {name: 'shadow-xl', value:'0 20px 50px rgba(6, 41, 81, 0.25)' ,usage: 'Modals, overlays'}
        ],
        motion: [
        {name: 'duration-fast', value:'150ms' ,usage: 'Micro-interactions, hover'},
        {name: 'duration-normal', value:'250ms' ,usage: 'Transitions, fade'},
        {name: 'duration-slow', value:'400ms' ,usage: 'Page transitions, modals'},
        {name: 'duration-standard', value:'cubic-bezier(0.2, 0, 0, 1)' ,usage: 'Default easing'},
        ],
        breakpoints:[
        {name: 'mobile', value: '0-767px',usage:'Phones'},
        {name: 'tablet', value: '768-1023px',usage:'Tablets'},
        {name: 'desktop', value: '1024-1439px',usage:'Laptops'},
        {name: 'wide', value: '1440px+',usage:'Large screens'},

        ]
    };

    buttonVariants = [
        {name:'Primary', class: 'btn-primary',bg:'#1983C2',text:'#FFFFFF'},
        {name:'Secondary', class: 'btn-secondary',bg:'transparent',text:'#1983C2'},
        {name:'Success', class: 'btn-success',bg:'#268B42',text:'#FFFFFF'},
        {name:'Danger', class: 'btn-danger',bg:'#C0392B',text:'#FFFFFF'},
    ];

    buttonStates = ['default','hover','active','focus','disabled'];

    changelog =[
        {area:'Navigation',before:'Event List,Levels,Solver in top nav',after:'Hackathons as central hub',rationale:'Better user flow, hackathons are the primary entity'},
        {area:'Colours',before:'Basic palette',after:'WCAG 2.2 compliant palette',rationale:'Accessibility requirements'},
        {area:'Typography',before:'System fonts',after:'Inter from Google Fonts',rationale:'Better readability, more refined'},
        {area:'Buttons',before:'Three variants',after:'Five variants with sizes',rationale:'More flexibility, consistent patterns'},
        {area:'Hackathons',before:'Not present',after:'Full CRUD grid cards',rationale:'Client requirement, central feature'},
        {area:'Dates',before:'Required fields',after:'Removed from hackathon creation',rationale:'Simplification based on user feedback'},
        {area:'Back Navigation',before:'None',after:'Consistent across all pages',rationale:'Improved UX flow'},
        {area:'Accessibility',before:'Basic consideration',after:'WCAG 2.2 AA target',rationale:'Mandatory requirement'},
    ];

    wcagRatings = [
    {pair: '#13192D on #FFFFFF', ratio:'17.3:1',compliance:'AAA'},
    {pair: '#3A4357 on #FFFFFF', ratio:'11.2:1',compliance:'AAA'},
    {pair: '#1983C2 on #FFFFFF', ratio:'5.8:1',compliance:'AA'},
    {pair: '#268B42 on #FFFFFF', ratio:'6.2:1',compliance:'AA'},
    {pair: '#FFFFFF on #13192D', ratio:'17.3:1',compliance:'AAA'},
    {pair: '#FFFFFF on #062951', ratio:'14.5:1',compliance:'AAA'}
    ];

    activeSection ='brand-foundation';
    isMenuOpen = false;

    sections = [
    {id: 'brand-foundation',label:'Brand Foundation',icon:'B'},
    {id: 'colour-palette',label:'Colour Palette',icon:'C'},
    {id: 'typography',label:'Typography',icon:'T'},
    {id: 'logo-iconography',label:'Logo & Iconography',icon:'L'},
    {id: 'design-tokens',label:'Design Tokens',icon:'D'},
    {id: 'components',label:'Components',icon:'M'},
    {id: 'layout-spacing',label:'Layout & Spacing',icon:'S'},
    {id: 'accessibility',label:'Accessibility',icon:'A'},
    {id: 'voice-tone',label:'Voice & Tone',icon:'V'},
    {id: 'changelog',label:'Changelog',icon:'H'},

    ];



    scrollToSection(sectionId: string):void {
        this.activeSection = sectionId;
        this.isMenuOpen = false;
        
        const element = document.getElementById(sectionId);
        if (element){
            element.scrollIntoView({behavior:'smooth',block:'start'});
        }
    }
    getColorPreview(hex: string): SafeHtml {
        const isValidHex = /^#[0-9A-Fa-f]{6}$/.test(hex);
        if(!isValidHex){
        return this.sanitizer.bypassSecurityTrustHtml(
            '<span style = "display : inline-block; width:24px ; height: 24px; border-radius: 4px; background:'+ hex+';border:1px solid #e7eaf0; "></span>'
        );
    }
    return this.sanitizer.bypassSecurityTrustHtml(
        '<span style="display:inline-block;width:24px;height:24px;border-radius:4px;background:' + hex + ';border:1px solid #e7eaf0;"></span>'

    );

} 
}