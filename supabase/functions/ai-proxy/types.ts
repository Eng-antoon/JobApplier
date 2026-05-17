export interface ActionRequest {
  action: string;
  payload: Record<string, unknown>;
}

export type ActionResponse = Record<string, unknown>;

