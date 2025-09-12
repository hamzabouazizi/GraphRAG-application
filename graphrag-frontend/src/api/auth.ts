import axios from 'axios';


export const login = async (email: string, password: string) => {
  const response = await axios.post(`/login`, { email, password });
  return response.data;
};

export const signup = async (email: string, password: string) => {
  const response = await axios.post(`/signup`, { email, password });
  return response.data;
};

export const getProfile = async (token: string) => {
  const response = await axios.get(`/profile`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return response.data;
};
