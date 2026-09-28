import { buildModule } from "@nomicfoundation/hardhat-ignition/modules";

export default buildModule("CertificateRegistry", (m) => {
  const admin = m.getParameter<string>("admin");
  const issuer = m.getParameter<string>("issuer");
  const registry = m.contract("CertificateRegistry", [admin, issuer]);
  return { registry };
});
