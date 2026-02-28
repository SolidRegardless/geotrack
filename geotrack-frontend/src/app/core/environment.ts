const isDocker = typeof window !== 'undefined' && window.location.port !== '4200';

export const environment = isDocker ? {
  production: true,
  apiUrl: '/api/v1',
  wsUrl: 'ws://' + window.location.host + '/ws/tracking'
} : {
  production: false,
  apiUrl: 'http://localhost:8080/api/v1',
  wsUrl: 'ws://localhost:8080/ws/tracking'
};
