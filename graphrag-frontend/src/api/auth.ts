import axios from 'axios';

const BASE_URL = process.env.REACT_APP_USER_MGMT_URL as string;

export const login = async (email: string, password: string) => {
  const response = await axios.post(`${BASE_URL}/login`, { email, password });
  return response.data;
};

export const signup = async (email: string, password: string) => {
  const response = await axios.post(`${BASE_URL}/signup`, { email, password });
  return response.data;
};

export const getProfile = async (token: string) => {
  const response = await axios.get(`${BASE_URL}/profile`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return response.data;
};
