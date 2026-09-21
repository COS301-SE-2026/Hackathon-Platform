export interface EventTimer {
  label: string;
  days: string;
  hours: string;
  minutes: string;
  seconds: string;
}

export function calculateEventTimer( startDateTime: string, duration: number): EventTimer {

  const now = new Date();
  
  const start = new Date(startDateTime);

  const end = new Date(start.getTime() + duration * 60 * 60 * 1000);

  let target: Date;
  let label: string;

  if (now < start) { target = start; label = 'Starts in'; } 
  else { target = end;label = 'Time Remaining';}

  const diff = Math.max( 0,target.getTime() - now.getTime());

  const days = Math.floor( diff / (1000 * 60 * 60 * 24));

  const hours = Math.floor( (diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60) );

  const minutes = Math.floor( (diff % (1000 * 60 * 60)) / (1000 * 60));

  const seconds = Math.floor( (diff % (1000 * 60)) / 1000);

  return { label, days: String(days).padStart(2, '0'), hours: String(hours).padStart(2, '0'), minutes: String(minutes).padStart(2, '0'), seconds: String(seconds).padStart(2, '0')};
}