export type Callback = {
  actions?: {
    type: number;
    userid: string;
  };
  key: string;
  url?: string;
  status: number;
  users?: string[];
};
