package com.certchain.blockchain.generated;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.CustomError;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.StaticStruct;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint48;
import org.web3j.abi.datatypes.generated.Uint64;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple2;
import org.web3j.tuples.generated.Tuple4;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the
 * <a href="https://github.com/LFDT-web3j/web3j/tree/main/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 5.0.3.
 */
@SuppressWarnings("rawtypes")
@Generated("org.web3j.codegen.SolidityFunctionWrapperGenerator")
public class CertificateRegistry extends Contract {
    public static final String BINARY = "0x608060405234801561000f575f5ffd5b50604051612bba380380612bba83398181016040528101906100319190610639565b620151806100448361021860201b60201c565b5f73ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff16036100b4575f6040517fc22c80220000000000000000000000000000000000000000000000000000000081526004016100ab9190610686565b60405180910390fd5b816001601a6101000a81548165ffffffffffff021916908365ffffffffffff1602179055506100eb5f5f1b8261031360201b60201c565b50505061011d7f9b8653b66f8f5f22353946a6bbf860bec6a380e40da7cbc79ae1d4a1c9f70ab66103e860201b60201c565b5f73ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff16036101b15761017f7f7cbf408b46658d18a3b4eb79784f6e7015577ddcd68aefe3eaa55c20e4ecd54a6103e860201b60201c565b6040517fd92e233d00000000000000000000000000000000000000000000000000000000815260040160405180910390fd5b6101e07f27183cc0c2c3c94d73e18ab819fd84507e3df25cc0f12db382928f91f421c9ae6103e860201b60201c565b6102107f114e74f6ea3bd819998f78687bfcb11b140da08e9b7d222fa9c1f1ba1f2aa1228261031360201b60201c565b5050506106cc565b5f6102487f2c08711b06b89b705dcc6f18435c58280891ec8b309e4bda9743bbcb71e842c76103e860201b60201c565b5f73ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff16036102dc576102aa7f9c77c172bce76eb1d056a3d4d6b0e4490b2eae9ebd731e3e9b52d26d55440e0e6103e860201b60201c565b6040517fd92e233d00000000000000000000000000000000000000000000000000000000815260040160405180910390fd5b61030b7f2c906903ac0121c926dda7dd52b167626f66d3ea96008280aabb78980e6d0a956103e860201b60201c565b819050919050565b5f5f5f1b83036103d0575f73ffffffffffffffffffffffffffffffffffffffff1661034261040c60201b60201c565b73ffffffffffffffffffffffffffffffffffffffff161461038f576040517f3fc3c27a00000000000000000000000000000000000000000000000000000000815260040160405180910390fd5b8160025f6101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055505b6103e0838361043460201b60201c565b905092915050565b6104098161040461052960201b6113bb1761055560201b60201c565b60201c565b50565b5f60025f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff16905090565b5f610445838361056760201b60201c565b61051f5760015f5f8581526020019081526020015f205f015f8473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f6101000a81548160ff0219169083151502179055506104bc6105ca60201b60201c565b73ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff16847f2f8788117e7eff1d82e926ec794901d17c78024a50270940304540a733656f0d60405160405180910390a460019050610523565b5f90505b92915050565b5f73c0bec0bec0bec0bec0bec0bec0bec0bec0bec0be90506040518281525f5f602083855afa50505050565b6105d160201b611daf17819050919050565b5f5f5f8481526020019081526020015f205f015f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f9054906101000a900460ff16905092915050565b5f33905090565b6105d961069f565b565b5f5ffd5b5f73ffffffffffffffffffffffffffffffffffffffff82169050919050565b5f610608826105df565b9050919050565b610618816105fe565b8114610622575f5ffd5b50565b5f815190506106338161060f565b92915050565b5f5f6040838503121561064f5761064e6105db565b5b5f61065c85828601610625565b925050602061066d85828601610625565b9150509250929050565b610680816105fe565b82525050565b5f6020820190506106995f830184610677565b92915050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52605160045260245ffd5b6124e1806106d95f395ff3fe608060405234801561000f575f5ffd5b506004361061014b575f3560e01c80638b3be14a116100c1578063cc8463c81161007a578063cc8463c814610379578063cefc142914610397578063cf6eefb7146103a1578063d547741f146103c0578063d602b9fd146103dc578063f333fe08146103e65761014b565b80638b3be14a146102b65780638da5cb5b146102d257806391d14854146102f0578063a1eda53c14610320578063a217fddf1461033f578063c6cbc52a1461035d5761014b565b80632f2ff15d116101135780632f2ff15d1461020a57806336568abe14610226578063634e93da14610242578063649a5ec71461025e57806382aefa241461027a57806384ef8ffc146102985761014b565b806301ffc9a71461014f578063022d63fb1461017f5780630aa6220b1461019d5780631f75435d146101a7578063248a9ca3146101da575b5f5ffd5b61016960048036038101906101649190611e68565b610416565b6040516101769190611ead565b60405180910390f35b61018761048f565b6040516101949190611ee6565b60405180910390f35b6101a5610499565b005b6101c160048036038101906101bc9190611f32565b6104b0565b6040516101d19493929190611f70565b60405180910390f35b6101f460048036038101906101ef9190611fb3565b6106a1565b6040516102019190611fed565b60405180910390f35b610224600480360381019061021f9190612060565b6106bd565b005b610240600480360381019061023b9190612060565b610706565b005b61025c6004803603810190610257919061209e565b610818565b005b610278600480360381019061027391906120f3565b610831565b005b61028261084a565b60405161028f9190611fed565b60405180910390f35b6102a061086e565b6040516102ad919061212d565b60405180910390f35b6102d060048036038101906102cb9190612183565b610896565b005b6102da610d8c565b6040516102e7919061212d565b60405180910390f35b61030a60048036038101906103059190612060565b610d9a565b6040516103179190611ead565b60405180910390f35b610328610dfd565b6040516103369291906121d3565b60405180910390f35b610347610e5b565b6040516103549190611fed565b60405180910390f35b61037760048036038101906103729190611fb3565b610e61565b005b6103816110e8565b60405161038e9190611ee6565b60405180910390f35b61039f611155565b005b6103a96111ea565b6040516103b79291906121fa565b60405180910390f35b6103da60048036038101906103d59190612060565b61122b565b005b6103e4611274565b005b61040060048036038101906103fb9190611fb3565b61128b565b60405161040d91906122c3565b60405180910390f35b5f7f31498786000000000000000000000000000000000000000000000000000000007bffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916827bffffffffffffffffffffffffffffffffffffffffffffffffffffffff191614806104885750610487826113e7565b5b9050919050565b5f62069780905090565b5f5f1b6104a581611460565b6104ad611474565b50565b5f5f5f5f6104dd7f2df5c4f79ffce6d9f5441404092d8bc4133140f645645b8c66f6376849c9cdff611480565b5f60035f8881526020019081526020015f20905061051a7fe528fda2049b2a82af03c684a5fd2861617363c93a633173d7ce478efa2a6316611480565b5f816001015f9054906101000a900467ffffffffffffffff1667ffffffffffffffff161415945061056a7ff4d0341a6b7ff43f3b422338e3d68f5d6a255bb5d17a4224da9fd0ebf4d3ede0611480565b846105aa576105987f88821d19a328530aca0c9487a1190db83aa3661f2373efae729f955caba0904f611480565b5f5f5f5f945094509450945050610698565b6105d37f681d93034011d748c8d2bc1cd6fb20092adacc27b23120f31158d240f6eb3356611480565b85815f01541493506106047f68e0ce0fe1ede846c81f4a72cdaa96a2148c32663d29e999e35d0c0b0b0f0055611480565b8060010160109054906101000a900460ff1692506106417f606efe6abf39be719a4bb79b04a2aa05b5f6d3dd79f7b7370f797e058c562159611480565b5f8160010160089054906101000a900467ffffffffffffffff1667ffffffffffffffff161415801561069457508060010160089054906101000a900467ffffffffffffffff1667ffffffffffffffff1642115b9150505b92959194509250565b5f5f5f8381526020019081526020015f20600101549050919050565b5f5f1b82036106f8576040517f3fc3c27a00000000000000000000000000000000000000000000000000000000815260040160405180910390fd5b610702828261149a565b5050565b5f5f1b82148015610749575061071a61086e565b73ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff16145b1561080a575f5f6107586111ea565b915091505f73ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff1614158061079d575061079b816114bc565b155b806107ae57506107ac816114d0565b155b156107f057806040517f19ca5ebb0000000000000000000000000000000000000000000000000000000081526004016107e79190611ee6565b60405180910390fd5b600160146101000a81549065ffffffffffff021916905550505b61081482826114e3565b5050565b5f5f1b61082481611460565b61082d8261155e565b5050565b5f5f1b61083d81611460565b610846826115d8565b5050565b7f114e74f6ea3bd819998f78687bfcb11b140da08e9b7d222fa9c1f1ba1f2aa12281565b5f60025f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff16905090565b7f114e74f6ea3bd819998f78687bfcb11b140da08e9b7d222fa9c1f1ba1f2aa1226108c081611460565b6108e97f5b3dc54b64c04a39464282a9dcab04f99d3f0c20b522c617dcffec2f5a9716d4611480565b5f5f1b840361094d5761091b7fcc540d904006e75bb9e593bb6012cc7a4bb8aa146c5a3963c975982ca22146a8611480565b6040517f4448b67e00000000000000000000000000000000000000000000000000000000815260040160405180910390fd5b6109767f4909a3323647cae16c741f92d4b6a7edb3e24f37d6c5eeedb0a6288a8d729262611480565b5f5f1b83036109da576109a87f5c3223cfa48774e32446e41cc83b0b4c81759bfcb3f30856e687f5278b1653ea611480565b6040517f7cf5d4a500000000000000000000000000000000000000000000000000000000815260040160405180910390fd5b610a037f47f11a48b985d746d29d5d1d878ab206a1d4567a98bc5db24eb7902e47cdcb9d611480565b5f60035f8681526020019081526020015f206001015f9054906101000a900467ffffffffffffffff1667ffffffffffffffff1614610aca57610a647f4f013b54cb47c2cc531b002e87f1fe8a52532c1e00681b04021b27957c056cf9611480565b610a8d7ff47847a352387f8b191884a1f33ff5f943350a9f10760d81036bca8147d634a8611480565b836040517f0ddf9dbe000000000000000000000000000000000000000000000000000000008152600401610ac19190611fed565b60405180910390fd5b610af37fce505b5b4bc405f8d39db697e0b230e8e52104899415adbb97dfc0a4200940be611480565b5f8267ffffffffffffffff1614158015610b175750428267ffffffffffffffff1611155b15610b8257610b457ff892c1662bd6d682f11d1f084f2cea9612f407fb863a15be63faf1a5692bd7eb611480565b816040517f5e23ca68000000000000000000000000000000000000000000000000000000008152600401610b7991906122eb565b60405180910390fd5b610bab7ffce708d8ce4ac5a9ced379e9182233b7076f0426f6b0e3c6d35483ba6a4941c3611480565b5f429050610bd87f0c2ac361d413f8ea93d0442e1e6eccd86aec1a03d7ea03014264d6e287e2c818611480565b6040518060a001604052808581526020018267ffffffffffffffff1681526020018467ffffffffffffffff1681526020015f151581526020013373ffffffffffffffffffffffffffffffffffffffff1681525060035f8781526020019081526020015f205f820151815f01556020820151816001015f6101000a81548167ffffffffffffffff021916908367ffffffffffffffff16021790555060408201518160010160086101000a81548167ffffffffffffffff021916908367ffffffffffffffff16021790555060608201518160010160106101000a81548160ff0219169083151502179055506080820151816002015f6101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550905050610d337f0c4b8abfaaa4ea01194d6104a52b44af5715fac817d24e4fde488fbc2f6a5ca8611480565b3373ffffffffffffffffffffffffffffffffffffffff1684867fe1e68a6175f44ef852dfec1ca2ccba8322ccc059c33b61f1c40baf243f912d358487604051610d7d929190612304565b60405180910390a45050505050565b5f610d9561086e565b905090565b5f5f5f8481526020019081526020015f205f015f8373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f9054906101000a900460ff16905092915050565b5f5f6002601a9054906101000a900465ffffffffffff169050610e1f816114bc565b8015610e315750610e2f816114d0565b155b610e3c575f5f610e53565b600260149054906101000a900465ffffffffffff16815b915091509091565b5f5f1b81565b7f114e74f6ea3bd819998f78687bfcb11b140da08e9b7d222fa9c1f1ba1f2aa122610e8b81611460565b610eb47f5524268736f0ece24532baa0720666814a83074d4e6202d7df39b9accbf878d6611480565b5f60035f8481526020019081526020015f209050610ef17f735a9eeac4a86f4da8b5ab8a9878f73daa6245970b10d79ae44db2259e64abcb611480565b5f816001015f9054906101000a900467ffffffffffffffff1667ffffffffffffffff1603610f7f57610f427fafeb5480a1177dfc701a4207b7e407c0ebdd006c679585c52c7155c14208b777611480565b826040517f7585e638000000000000000000000000000000000000000000000000000000008152600401610f769190611fed565b60405180910390fd5b610fa87f01d82eb183e6cacca0a87d970036c8cc90954b4cd58d06d17841f31889b33eba611480565b8060010160109054906101000a900460ff161561102557610fe87f4020cac883cd41cfdd7dd7be4e1f03c5f20bef9b2deaa8313fba5ee35f693b25611480565b826040517fb93e450d00000000000000000000000000000000000000000000000000000000815260040161101c9190611fed565b60405180910390fd5b61104e7f2cd54a998120ca96562b083b7fa48e9a39eaf2312272fa32ec2b031daad5e120611480565b60018160010160106101000a81548160ff0219169083151502179055506110947fde47937480dab1feb7315cdc52f3068a11c4c0436ee71bce4595ef0a68dd8d80611480565b3373ffffffffffffffffffffffffffffffffffffffff16837f86e31385c8dde141636e5662d2638d6e541f0db140a8fcbce62a6d62c7b103bb426040516110db91906122eb565b60405180910390a3505050565b5f5f6002601a9054906101000a900465ffffffffffff16905061110a816114bc565b801561111b575061111a816114d0565b5b611139576001601a9054906101000a900465ffffffffffff1661114f565b600260149054906101000a900465ffffffffffff165b91505090565b5f61115e6111ea565b5090508073ffffffffffffffffffffffffffffffffffffffff1661118061163e565b73ffffffffffffffffffffffffffffffffffffffff16146111df576111a361163e565b6040517fc22c80220000000000000000000000000000000000000000000000000000000081526004016111d6919061212d565b60405180910390fd5b6111e7611645565b50565b5f5f60015f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff16600160149054906101000a900465ffffffffffff16915091509091565b5f5f1b8203611266576040517f3fc3c27a00000000000000000000000000000000000000000000000000000000815260040160405180910390fd5b6112708282611710565b5050565b5f5f1b61128081611460565b611288611732565b50565b611293611db9565b6112bc7f83c326ee29406b3e5375cb85a822a8cacaf523900ea6f22bf909462668190383611480565b60035f8381526020019081526020015f206040518060a00160405290815f8201548152602001600182015f9054906101000a900467ffffffffffffffff1667ffffffffffffffff1667ffffffffffffffff1681526020016001820160089054906101000a900467ffffffffffffffff1667ffffffffffffffff1667ffffffffffffffff1681526020016001820160109054906101000a900460ff16151515158152602001600282015f9054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815250509050919050565b5f73c0bec0bec0bec0bec0bec0bec0bec0bec0bec0be90506040518281525f5f602083855afa50505050565b5f7f7965db0b000000000000000000000000000000000000000000000000000000007bffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916827bffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916148061145957506114588261173e565b5b9050919050565b6114718161146c61163e565b6117a7565b50565b61147e5f5f6117f8565b565b6114978161148f6113bb6118e7565b63ffffffff16565b50565b6114a3826106a1565b6114ac81611460565b6114b683836118f2565b50505050565b5f5f8265ffffffffffff1614159050919050565b5f428265ffffffffffff16109050919050565b6114eb61163e565b73ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff161461154f576040517f6697b23200000000000000000000000000000000000000000000000000000000815260040160405180910390fd5b61155982826119bb565b505050565b5f6115676110e8565b61157042611a3b565b61157a9190612358565b90506115868282611a94565b8173ffffffffffffffffffffffffffffffffffffffff167f3377dc44241e779dd06afab5b788a35ca5f3b778836e2990bdb26a2a4b2e5ed6826040516115cc9190611ee6565b60405180910390a25050565b5f6115e282611b45565b6115eb42611a3b565b6115f59190612358565b905061160182826117f8565b7ff1038c18cf84a56e432fdbfaf746924b7ea511dfe03a6506a0ceba4888788d9b82826040516116329291906121d3565b60405180910390a15050565b5f33905090565b5f5f61164f6111ea565b9150915061165c816114bc565b158061166e575061166c816114d0565b155b156116b057806040517f19ca5ebb0000000000000000000000000000000000000000000000000000000081526004016116a79190611ee6565b60405180910390fd5b6116c35f5f1b6116be61086e565b6119bb565b506116d05f5f1b836118f2565b5060015f6101000a81549073ffffffffffffffffffffffffffffffffffffffff0219169055600160146101000a81549065ffffffffffff02191690555050565b611719826106a1565b61172281611460565b61172c83836119bb565b50505050565b61173c5f5f611a94565b565b5f7f01ffc9a7000000000000000000000000000000000000000000000000000000007bffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916827bffffffffffffffffffffffffffffffffffffffffffffffffffffffff1916149050919050565b6117b18282610d9a565b6117f45780826040517fe2517d3f0000000000000000000000000000000000000000000000000000000081526004016117eb929190612391565b60405180910390fd5b5050565b5f6002601a9054906101000a900465ffffffffffff169050611819816114bc565b1561189857611827816114d0565b1561186a57600260149054906101000a900465ffffffffffff166001601a6101000a81548165ffffffffffff021916908365ffffffffffff160217905550611897565b7f2b1fa2edafe6f7b9e97c1a9e0c3660e645beb2dcaa2d45bdbf9beaf5472e1ec560405160405180910390a15b5b82600260146101000a81548165ffffffffffff021916908365ffffffffffff160217905550816002601a6101000a81548165ffffffffffff021916908365ffffffffffff160217905550505050565b611daf819050919050565b5f5f5f1b83036119a9575f73ffffffffffffffffffffffffffffffffffffffff1661191b61086e565b73ffffffffffffffffffffffffffffffffffffffff1614611968576040517f3fc3c27a00000000000000000000000000000000000000000000000000000000815260040160405180910390fd5b8160025f6101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055505b6119b38383611ba3565b905092915050565b5f5f5f1b831480156119ff57506119d061086e565b73ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff16145b15611a295760025f6101000a81549073ffffffffffffffffffffffffffffffffffffffff02191690555b611a338383611c8c565b905092915050565b5f65ffffffffffff8016821115611a8c576030826040517f6dfcc650000000000000000000000000000000000000000000000000000000008152600401611a8392919061241e565b60405180910390fd5b819050919050565b5f611a9d6111ea565b9150508260015f6101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555081600160146101000a81548165ffffffffffff021916908365ffffffffffff160217905550611b0e816114bc565b15611b40577f8886ebfc4259abdbc16601dd8fb5678e54878f47b3c34836cfc51154a960510960405160405180910390a15b505050565b5f5f611b4f6110e8565b90508065ffffffffffff168365ffffffffffff1611611b79578281611b749190612445565b611b9b565b611b9a8365ffffffffffff16611b8d61048f565b65ffffffffffff16611d75565b5b915050919050565b5f611bae8383610d9a565b611c825760015f5f8581526020019081526020015f205f015f8473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f6101000a81548160ff021916908315150217905550611c1f61163e565b73ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff16847f2f8788117e7eff1d82e926ec794901d17c78024a50270940304540a733656f0d60405160405180910390a460019050611c86565b5f90505b92915050565b5f611c978383610d9a565b15611d6b575f5f5f8581526020019081526020015f205f015f8473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020015f205f6101000a81548160ff021916908315150217905550611d0861163e565b73ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff16847ff6391f5c32d9c69d2a47ea670b442974b53935d1edc7fd64eb21e047a839171b60405160405180910390a460019050611d6f565b5f90505b92915050565b5f611d838284108484611d8b565b905092915050565b5f611d9584611da4565b82841802821890509392505050565b5f8115159050919050565b611db761247e565b565b6040518060a001604052805f81526020015f67ffffffffffffffff1681526020015f67ffffffffffffffff1681526020015f151581526020015f73ffffffffffffffffffffffffffffffffffffffff1681525090565b5f5ffd5b5f7fffffffff0000000000000000000000000000000000000000000000000000000082169050919050565b611e4781611e13565b8114611e51575f5ffd5b50565b5f81359050611e6281611e3e565b92915050565b5f60208284031215611e7d57611e7c611e0f565b5b5f611e8a84828501611e54565b91505092915050565b5f8115159050919050565b611ea781611e93565b82525050565b5f602082019050611ec05f830184611e9e565b92915050565b5f65ffffffffffff82169050919050565b611ee081611ec6565b82525050565b5f602082019050611ef95f830184611ed7565b92915050565b5f819050919050565b611f1181611eff565b8114611f1b575f5ffd5b50565b5f81359050611f2c81611f08565b92915050565b5f5f60408385031215611f4857611f47611e0f565b5b5f611f5585828601611f1e565b9250506020611f6685828601611f1e565b9150509250929050565b5f608082019050611f835f830187611e9e565b611f906020830186611e9e565b611f9d6040830185611e9e565b611faa6060830184611e9e565b95945050505050565b5f60208284031215611fc857611fc7611e0f565b5b5f611fd584828501611f1e565b91505092915050565b611fe781611eff565b82525050565b5f6020820190506120005f830184611fde565b92915050565b5f73ffffffffffffffffffffffffffffffffffffffff82169050919050565b5f61202f82612006565b9050919050565b61203f81612025565b8114612049575f5ffd5b50565b5f8135905061205a81612036565b92915050565b5f5f6040838503121561207657612075611e0f565b5b5f61208385828601611f1e565b92505060206120948582860161204c565b9150509250929050565b5f602082840312156120b3576120b2611e0f565b5b5f6120c08482850161204c565b91505092915050565b6120d281611ec6565b81146120dc575f5ffd5b50565b5f813590506120ed816120c9565b92915050565b5f6020828403121561210857612107611e0f565b5b5f612115848285016120df565b91505092915050565b61212781612025565b82525050565b5f6020820190506121405f83018461211e565b92915050565b5f67ffffffffffffffff82169050919050565b61216281612146565b811461216c575f5ffd5b50565b5f8135905061217d81612159565b92915050565b5f5f5f6060848603121561219a57612199611e0f565b5b5f6121a786828701611f1e565b93505060206121b886828701611f1e565b92505060406121c98682870161216f565b9150509250925092565b5f6040820190506121e65f830185611ed7565b6121f36020830184611ed7565b9392505050565b5f60408201905061220d5f83018561211e565b61221a6020830184611ed7565b9392505050565b61222a81611eff565b82525050565b61223981612146565b82525050565b61224881611e93565b82525050565b61225781612025565b82525050565b60a082015f8201516122715f850182612221565b5060208201516122846020850182612230565b5060408201516122976040850182612230565b5060608201516122aa606085018261223f565b5060808201516122bd608085018261224e565b50505050565b5f60a0820190506122d65f83018461225d565b92915050565b6122e581612146565b82525050565b5f6020820190506122fe5f8301846122dc565b92915050565b5f6040820190506123175f8301856122dc565b61232460208301846122dc565b9392505050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52601160045260245ffd5b5f61236282611ec6565b915061236d83611ec6565b9250828201905065ffffffffffff81111561238b5761238a61232b565b5b92915050565b5f6040820190506123a45f83018561211e565b6123b16020830184611fde565b9392505050565b5f819050919050565b5f60ff82169050919050565b5f819050919050565b5f6123f06123eb6123e6846123b8565b6123cd565b6123c1565b9050919050565b612400816123d6565b82525050565b5f819050919050565b61241881612406565b82525050565b5f6040820190506124315f8301856123f7565b61243e602083018461240f565b9392505050565b5f61244f82611ec6565b915061245a83611ec6565b9250828203905065ffffffffffff8111156124785761247761232b565b5b92915050565b7f4e487b71000000000000000000000000000000000000000000000000000000005f52605160045260245ffdfea2646970667358221220d7277a8f0d2cc81111fddc27e773242513b431dda5f2df48accce53304dc2daa64736f6c63430008220033\r\n";

    private static String librariesLinkedBinary;

    public static final String FUNC_DEFAULT_ADMIN_ROLE = "DEFAULT_ADMIN_ROLE";

    public static final String FUNC_ISSUER_ROLE = "ISSUER_ROLE";

    public static final String FUNC_ACCEPTDEFAULTADMINTRANSFER = "acceptDefaultAdminTransfer";

    public static final String FUNC_BEGINDEFAULTADMINTRANSFER = "beginDefaultAdminTransfer";

    public static final String FUNC_CANCELDEFAULTADMINTRANSFER = "cancelDefaultAdminTransfer";

    public static final String FUNC_CHANGEDEFAULTADMINDELAY = "changeDefaultAdminDelay";

    public static final String FUNC_DEFAULTADMIN = "defaultAdmin";

    public static final String FUNC_DEFAULTADMINDELAY = "defaultAdminDelay";

    public static final String FUNC_DEFAULTADMINDELAYINCREASEWAIT = "defaultAdminDelayIncreaseWait";

    public static final String FUNC_GETCERTIFICATE = "getCertificate";

    public static final String FUNC_GETROLEADMIN = "getRoleAdmin";

    public static final String FUNC_GRANTROLE = "grantRole";

    public static final String FUNC_HASROLE = "hasRole";

    public static final String FUNC_ISSUECERTIFICATE = "issueCertificate";

    public static final String FUNC_OWNER = "owner";

    public static final String FUNC_PENDINGDEFAULTADMIN = "pendingDefaultAdmin";

    public static final String FUNC_PENDINGDEFAULTADMINDELAY = "pendingDefaultAdminDelay";

    public static final String FUNC_RENOUNCEROLE = "renounceRole";

    public static final String FUNC_REVOKECERTIFICATE = "revokeCertificate";

    public static final String FUNC_REVOKEROLE = "revokeRole";

    public static final String FUNC_ROLLBACKDEFAULTADMINDELAY = "rollbackDefaultAdminDelay";

    public static final String FUNC_SUPPORTSINTERFACE = "supportsInterface";

    public static final String FUNC_VERIFYCERTIFICATE = "verifyCertificate";

    public static final CustomError ACCESSCONTROLBADCONFIRMATION_ERROR = new CustomError("AccessControlBadConfirmation",
            Arrays.<TypeReference<?>>asList());
    ;

    public static final CustomError ACCESSCONTROLENFORCEDDEFAULTADMINDELAY_ERROR = new CustomError("AccessControlEnforcedDefaultAdminDelay",
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint48>() {}));
    ;

    public static final CustomError ACCESSCONTROLENFORCEDDEFAULTADMINRULES_ERROR = new CustomError("AccessControlEnforcedDefaultAdminRules",
            Arrays.<TypeReference<?>>asList());
    ;

    public static final CustomError ACCESSCONTROLINVALIDDEFAULTADMIN_ERROR = new CustomError("AccessControlInvalidDefaultAdmin",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
    ;

    public static final CustomError ACCESSCONTROLUNAUTHORIZEDACCOUNT_ERROR = new CustomError("AccessControlUnauthorizedAccount",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Bytes32>() {}));
    ;

    public static final CustomError CERTIFICATEALREADYISSUED_ERROR = new CustomError("CertificateAlreadyIssued",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
    ;

    public static final CustomError CERTIFICATEALREADYREVOKED_ERROR = new CustomError("CertificateAlreadyRevoked",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
    ;

    public static final CustomError CERTIFICATENOTFOUND_ERROR = new CustomError("CertificateNotFound",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
    ;

    public static final CustomError INVALIDEXPIRY_ERROR = new CustomError("InvalidExpiry",
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint64>() {}));
    ;

    public static final CustomError SAFECASTOVERFLOWEDUINTDOWNCAST_ERROR = new CustomError("SafeCastOverflowedUintDowncast",
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final CustomError ZEROADDRESS_ERROR = new CustomError("ZeroAddress",
            Arrays.<TypeReference<?>>asList());
    ;

    public static final CustomError ZEROCERTIFICATEHASH_ERROR = new CustomError("ZeroCertificateHash",
            Arrays.<TypeReference<?>>asList());
    ;

    public static final CustomError ZEROCERTIFICATEKEY_ERROR = new CustomError("ZeroCertificateKey",
            Arrays.<TypeReference<?>>asList());
    ;

    public static final Event CERTIFICATEISSUED_EVENT = new Event("CertificateIssued",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Bytes32>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint64>() {}, new TypeReference<Uint64>() {}));
    ;

    public static final Event CERTIFICATEREVOKED_EVENT = new Event("CertificateRevoked",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint64>() {}));
    ;

    public static final Event DEFAULTADMINDELAYCHANGECANCELED_EVENT = new Event("DefaultAdminDelayChangeCanceled",
            Arrays.<TypeReference<?>>asList());
    ;

    public static final Event DEFAULTADMINDELAYCHANGESCHEDULED_EVENT = new Event("DefaultAdminDelayChangeScheduled",
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint48>() {}, new TypeReference<Uint48>() {}));
    ;

    public static final Event DEFAULTADMINTRANSFERCANCELED_EVENT = new Event("DefaultAdminTransferCanceled",
            Arrays.<TypeReference<?>>asList());
    ;

    public static final Event DEFAULTADMINTRANSFERSCHEDULED_EVENT = new Event("DefaultAdminTransferScheduled",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Uint48>() {}));
    ;

    public static final Event ROLEADMINCHANGED_EVENT = new Event("RoleAdminChanged",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Bytes32>(true) {}, new TypeReference<Bytes32>(true) {}));
    ;

    public static final Event ROLEGRANTED_EVENT = new Event("RoleGranted",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

    public static final Event ROLEREVOKED_EVENT = new Event("RoleRevoked",
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}));
    ;

    @Deprecated
    protected CertificateRegistry(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected CertificateRegistry(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected CertificateRegistry(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected CertificateRegistry(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<CertificateIssuedEventResponse> getCertificateIssuedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(CERTIFICATEISSUED_EVENT, transactionReceipt);
        ArrayList<CertificateIssuedEventResponse> responses = new ArrayList<CertificateIssuedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            CertificateIssuedEventResponse typedResponse = new CertificateIssuedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.certificateKey = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.certificateHash = (byte[]) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.issuer = (String) eventValues.getIndexedValues().get(2).getValue();
            typedResponse.issuedAt = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.expiresAt = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static CertificateIssuedEventResponse getCertificateIssuedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(CERTIFICATEISSUED_EVENT, log);
        CertificateIssuedEventResponse typedResponse = new CertificateIssuedEventResponse();
        typedResponse.log = log;
        typedResponse.certificateKey = (byte[]) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.certificateHash = (byte[]) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.issuer = (String) eventValues.getIndexedValues().get(2).getValue();
        typedResponse.issuedAt = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.expiresAt = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<CertificateIssuedEventResponse> certificateIssuedEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getCertificateIssuedEventFromLog(log));
    }

    public Flowable<CertificateIssuedEventResponse> certificateIssuedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(CERTIFICATEISSUED_EVENT));
        return certificateIssuedEventFlowable(filter);
    }

    public static List<CertificateRevokedEventResponse> getCertificateRevokedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(CERTIFICATEREVOKED_EVENT, transactionReceipt);
        ArrayList<CertificateRevokedEventResponse> responses = new ArrayList<CertificateRevokedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            CertificateRevokedEventResponse typedResponse = new CertificateRevokedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.certificateKey = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.revokedBy = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.revokedAt = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static CertificateRevokedEventResponse getCertificateRevokedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(CERTIFICATEREVOKED_EVENT, log);
        CertificateRevokedEventResponse typedResponse = new CertificateRevokedEventResponse();
        typedResponse.log = log;
        typedResponse.certificateKey = (byte[]) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.revokedBy = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.revokedAt = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<CertificateRevokedEventResponse> certificateRevokedEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getCertificateRevokedEventFromLog(log));
    }

    public Flowable<CertificateRevokedEventResponse> certificateRevokedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(CERTIFICATEREVOKED_EVENT));
        return certificateRevokedEventFlowable(filter);
    }

    public static List<DefaultAdminDelayChangeCanceledEventResponse> getDefaultAdminDelayChangeCanceledEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(DEFAULTADMINDELAYCHANGECANCELED_EVENT, transactionReceipt);
        ArrayList<DefaultAdminDelayChangeCanceledEventResponse> responses = new ArrayList<DefaultAdminDelayChangeCanceledEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            DefaultAdminDelayChangeCanceledEventResponse typedResponse = new DefaultAdminDelayChangeCanceledEventResponse();
            typedResponse.log = eventValues.getLog();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static DefaultAdminDelayChangeCanceledEventResponse getDefaultAdminDelayChangeCanceledEventFromLog(
            Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(DEFAULTADMINDELAYCHANGECANCELED_EVENT, log);
        DefaultAdminDelayChangeCanceledEventResponse typedResponse = new DefaultAdminDelayChangeCanceledEventResponse();
        typedResponse.log = log;
        return typedResponse;
    }

    public Flowable<DefaultAdminDelayChangeCanceledEventResponse> defaultAdminDelayChangeCanceledEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getDefaultAdminDelayChangeCanceledEventFromLog(log));
    }

    public Flowable<DefaultAdminDelayChangeCanceledEventResponse> defaultAdminDelayChangeCanceledEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(DEFAULTADMINDELAYCHANGECANCELED_EVENT));
        return defaultAdminDelayChangeCanceledEventFlowable(filter);
    }

    public static List<DefaultAdminDelayChangeScheduledEventResponse> getDefaultAdminDelayChangeScheduledEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(DEFAULTADMINDELAYCHANGESCHEDULED_EVENT, transactionReceipt);
        ArrayList<DefaultAdminDelayChangeScheduledEventResponse> responses = new ArrayList<DefaultAdminDelayChangeScheduledEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            DefaultAdminDelayChangeScheduledEventResponse typedResponse = new DefaultAdminDelayChangeScheduledEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.newDelay = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.effectSchedule = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static DefaultAdminDelayChangeScheduledEventResponse getDefaultAdminDelayChangeScheduledEventFromLog(
            Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(DEFAULTADMINDELAYCHANGESCHEDULED_EVENT, log);
        DefaultAdminDelayChangeScheduledEventResponse typedResponse = new DefaultAdminDelayChangeScheduledEventResponse();
        typedResponse.log = log;
        typedResponse.newDelay = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.effectSchedule = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<DefaultAdminDelayChangeScheduledEventResponse> defaultAdminDelayChangeScheduledEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getDefaultAdminDelayChangeScheduledEventFromLog(log));
    }

    public Flowable<DefaultAdminDelayChangeScheduledEventResponse> defaultAdminDelayChangeScheduledEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(DEFAULTADMINDELAYCHANGESCHEDULED_EVENT));
        return defaultAdminDelayChangeScheduledEventFlowable(filter);
    }

    public static List<DefaultAdminTransferCanceledEventResponse> getDefaultAdminTransferCanceledEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(DEFAULTADMINTRANSFERCANCELED_EVENT, transactionReceipt);
        ArrayList<DefaultAdminTransferCanceledEventResponse> responses = new ArrayList<DefaultAdminTransferCanceledEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            DefaultAdminTransferCanceledEventResponse typedResponse = new DefaultAdminTransferCanceledEventResponse();
            typedResponse.log = eventValues.getLog();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static DefaultAdminTransferCanceledEventResponse getDefaultAdminTransferCanceledEventFromLog(
            Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(DEFAULTADMINTRANSFERCANCELED_EVENT, log);
        DefaultAdminTransferCanceledEventResponse typedResponse = new DefaultAdminTransferCanceledEventResponse();
        typedResponse.log = log;
        return typedResponse;
    }

    public Flowable<DefaultAdminTransferCanceledEventResponse> defaultAdminTransferCanceledEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getDefaultAdminTransferCanceledEventFromLog(log));
    }

    public Flowable<DefaultAdminTransferCanceledEventResponse> defaultAdminTransferCanceledEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(DEFAULTADMINTRANSFERCANCELED_EVENT));
        return defaultAdminTransferCanceledEventFlowable(filter);
    }

    public static List<DefaultAdminTransferScheduledEventResponse> getDefaultAdminTransferScheduledEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(DEFAULTADMINTRANSFERSCHEDULED_EVENT, transactionReceipt);
        ArrayList<DefaultAdminTransferScheduledEventResponse> responses = new ArrayList<DefaultAdminTransferScheduledEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            DefaultAdminTransferScheduledEventResponse typedResponse = new DefaultAdminTransferScheduledEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.newAdmin = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.acceptSchedule = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static DefaultAdminTransferScheduledEventResponse getDefaultAdminTransferScheduledEventFromLog(
            Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(DEFAULTADMINTRANSFERSCHEDULED_EVENT, log);
        DefaultAdminTransferScheduledEventResponse typedResponse = new DefaultAdminTransferScheduledEventResponse();
        typedResponse.log = log;
        typedResponse.newAdmin = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.acceptSchedule = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
        return typedResponse;
    }

    public Flowable<DefaultAdminTransferScheduledEventResponse> defaultAdminTransferScheduledEventFlowable(
            EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getDefaultAdminTransferScheduledEventFromLog(log));
    }

    public Flowable<DefaultAdminTransferScheduledEventResponse> defaultAdminTransferScheduledEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(DEFAULTADMINTRANSFERSCHEDULED_EVENT));
        return defaultAdminTransferScheduledEventFlowable(filter);
    }

    public static List<RoleAdminChangedEventResponse> getRoleAdminChangedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ROLEADMINCHANGED_EVENT, transactionReceipt);
        ArrayList<RoleAdminChangedEventResponse> responses = new ArrayList<RoleAdminChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RoleAdminChangedEventResponse typedResponse = new RoleAdminChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.role = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.previousAdminRole = (byte[]) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.newAdminRole = (byte[]) eventValues.getIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static RoleAdminChangedEventResponse getRoleAdminChangedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ROLEADMINCHANGED_EVENT, log);
        RoleAdminChangedEventResponse typedResponse = new RoleAdminChangedEventResponse();
        typedResponse.log = log;
        typedResponse.role = (byte[]) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.previousAdminRole = (byte[]) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.newAdminRole = (byte[]) eventValues.getIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public Flowable<RoleAdminChangedEventResponse> roleAdminChangedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRoleAdminChangedEventFromLog(log));
    }

    public Flowable<RoleAdminChangedEventResponse> roleAdminChangedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ROLEADMINCHANGED_EVENT));
        return roleAdminChangedEventFlowable(filter);
    }

    public static List<RoleGrantedEventResponse> getRoleGrantedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ROLEGRANTED_EVENT, transactionReceipt);
        ArrayList<RoleGrantedEventResponse> responses = new ArrayList<RoleGrantedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RoleGrantedEventResponse typedResponse = new RoleGrantedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.role = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.account = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.sender = (String) eventValues.getIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static RoleGrantedEventResponse getRoleGrantedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ROLEGRANTED_EVENT, log);
        RoleGrantedEventResponse typedResponse = new RoleGrantedEventResponse();
        typedResponse.log = log;
        typedResponse.role = (byte[]) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.account = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.sender = (String) eventValues.getIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public Flowable<RoleGrantedEventResponse> roleGrantedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRoleGrantedEventFromLog(log));
    }

    public Flowable<RoleGrantedEventResponse> roleGrantedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ROLEGRANTED_EVENT));
        return roleGrantedEventFlowable(filter);
    }

    public static List<RoleRevokedEventResponse> getRoleRevokedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(ROLEREVOKED_EVENT, transactionReceipt);
        ArrayList<RoleRevokedEventResponse> responses = new ArrayList<RoleRevokedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            RoleRevokedEventResponse typedResponse = new RoleRevokedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.role = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.account = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.sender = (String) eventValues.getIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static RoleRevokedEventResponse getRoleRevokedEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(ROLEREVOKED_EVENT, log);
        RoleRevokedEventResponse typedResponse = new RoleRevokedEventResponse();
        typedResponse.log = log;
        typedResponse.role = (byte[]) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.account = (String) eventValues.getIndexedValues().get(1).getValue();
        typedResponse.sender = (String) eventValues.getIndexedValues().get(2).getValue();
        return typedResponse;
    }

    public Flowable<RoleRevokedEventResponse> roleRevokedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getRoleRevokedEventFromLog(log));
    }

    public Flowable<RoleRevokedEventResponse> roleRevokedEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ROLEREVOKED_EVENT));
        return roleRevokedEventFlowable(filter);
    }

    public RemoteFunctionCall<byte[]> DEFAULT_ADMIN_ROLE() {
        final Function function = new Function(FUNC_DEFAULT_ADMIN_ROLE,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<byte[]> ISSUER_ROLE() {
        final Function function = new Function(FUNC_ISSUER_ROLE,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<TransactionReceipt> acceptDefaultAdminTransfer() {
        final Function function = new Function(
                FUNC_ACCEPTDEFAULTADMINTRANSFER,
                Arrays.<Type>asList(),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> beginDefaultAdminTransfer(String newAdmin) {
        final Function function = new Function(
                FUNC_BEGINDEFAULTADMINTRANSFER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, newAdmin)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> cancelDefaultAdminTransfer() {
        final Function function = new Function(
                FUNC_CANCELDEFAULTADMINTRANSFER,
                Arrays.<Type>asList(),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> changeDefaultAdminDelay(BigInteger newDelay) {
        final Function function = new Function(
                FUNC_CHANGEDEFAULTADMINDELAY,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint48(newDelay)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> defaultAdmin() {
        final Function function = new Function(FUNC_DEFAULTADMIN,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<BigInteger> defaultAdminDelay() {
        final Function function = new Function(FUNC_DEFAULTADMINDELAY,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint48>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<BigInteger> defaultAdminDelayIncreaseWait() {
        final Function function = new Function(FUNC_DEFAULTADMINDELAYINCREASEWAIT,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint48>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<CertificateRecord> getCertificate(byte[] certificateKey) {
        final Function function = new Function(FUNC_GETCERTIFICATE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(certificateKey)),
                Arrays.<TypeReference<?>>asList(new TypeReference<CertificateRecord>() {}));
        return executeRemoteCallSingleValueReturn(function, CertificateRecord.class);
    }

    public RemoteFunctionCall<byte[]> getRoleAdmin(byte[] role) {
        final Function function = new Function(FUNC_GETROLEADMIN,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(role)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<TransactionReceipt> grantRole(byte[] role, String account) {
        final Function function = new Function(
                FUNC_GRANTROLE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(role),
                new org.web3j.abi.datatypes.Address(160, account)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Boolean> hasRole(byte[] role, String account) {
        final Function function = new Function(FUNC_HASROLE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(role),
                new org.web3j.abi.datatypes.Address(160, account)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<TransactionReceipt> issueCertificate(byte[] certificateKey,
            byte[] certificateHash, BigInteger expiresAt) {
        final Function function = new Function(
                FUNC_ISSUECERTIFICATE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(certificateKey),
                new org.web3j.abi.datatypes.generated.Bytes32(certificateHash),
                new org.web3j.abi.datatypes.generated.Uint64(expiresAt)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> owner() {
        final Function function = new Function(FUNC_OWNER,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<Tuple2<String, BigInteger>> pendingDefaultAdmin() {
        final Function function = new Function(FUNC_PENDINGDEFAULTADMIN,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Uint48>() {}));
        return new RemoteFunctionCall<Tuple2<String, BigInteger>>(function,
                new Callable<Tuple2<String, BigInteger>>() {
                    @Override
                    public Tuple2<String, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple2<String, BigInteger>(
                                (String) results.get(0).getValue(),
                                (BigInteger) results.get(1).getValue());
                    }
                });
    }

    public RemoteFunctionCall<Tuple2<BigInteger, BigInteger>> pendingDefaultAdminDelay() {
        final Function function = new Function(FUNC_PENDINGDEFAULTADMINDELAY,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint48>() {}, new TypeReference<Uint48>() {}));
        return new RemoteFunctionCall<Tuple2<BigInteger, BigInteger>>(function,
                new Callable<Tuple2<BigInteger, BigInteger>>() {
                    @Override
                    public Tuple2<BigInteger, BigInteger> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple2<BigInteger, BigInteger>(
                                (BigInteger) results.get(0).getValue(),
                                (BigInteger) results.get(1).getValue());
                    }
                });
    }

    public RemoteFunctionCall<TransactionReceipt> renounceRole(byte[] role, String account) {
        final Function function = new Function(
                FUNC_RENOUNCEROLE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(role),
                new org.web3j.abi.datatypes.Address(160, account)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> revokeCertificate(byte[] certificateKey) {
        final Function function = new Function(
                FUNC_REVOKECERTIFICATE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(certificateKey)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> revokeRole(byte[] role, String account) {
        final Function function = new Function(
                FUNC_REVOKEROLE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(role),
                new org.web3j.abi.datatypes.Address(160, account)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> rollbackDefaultAdminDelay() {
        final Function function = new Function(
                FUNC_ROLLBACKDEFAULTADMINDELAY,
                Arrays.<Type>asList(),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<Boolean> supportsInterface(byte[] interfaceId) {
        final Function function = new Function(FUNC_SUPPORTSINTERFACE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes4(interfaceId)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteFunctionCall<Tuple4<Boolean, Boolean, Boolean, Boolean>> verifyCertificate(
            byte[] certificateKey, byte[] expectedHash) {
        final Function function = new Function(FUNC_VERIFYCERTIFICATE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(certificateKey),
                new org.web3j.abi.datatypes.generated.Bytes32(expectedHash)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}, new TypeReference<Bool>() {}, new TypeReference<Bool>() {}, new TypeReference<Bool>() {}));
        return new RemoteFunctionCall<Tuple4<Boolean, Boolean, Boolean, Boolean>>(function,
                new Callable<Tuple4<Boolean, Boolean, Boolean, Boolean>>() {
                    @Override
                    public Tuple4<Boolean, Boolean, Boolean, Boolean> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple4<Boolean, Boolean, Boolean, Boolean>(
                                (Boolean) results.get(0).getValue(),
                                (Boolean) results.get(1).getValue(),
                                (Boolean) results.get(2).getValue(),
                                (Boolean) results.get(3).getValue());
                    }
                });
    }

    @Deprecated
    public static CertificateRegistry load(String contractAddress, Web3j web3j,
            Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new CertificateRegistry(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static CertificateRegistry load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new CertificateRegistry(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static CertificateRegistry load(String contractAddress, Web3j web3j,
            Credentials credentials, ContractGasProvider contractGasProvider) {
        return new CertificateRegistry(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static CertificateRegistry load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new CertificateRegistry(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<CertificateRegistry> deploy(Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider, String admin, String issuer) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, admin),
                new org.web3j.abi.datatypes.Address(160, issuer)));
        return deployRemoteCall(CertificateRegistry.class, web3j, credentials, contractGasProvider, getDeploymentBinary(), encodedConstructor);
    }

    public static RemoteCall<CertificateRegistry> deploy(Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider,
            String admin, String issuer) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, admin),
                new org.web3j.abi.datatypes.Address(160, issuer)));
        return deployRemoteCall(CertificateRegistry.class, web3j, transactionManager, contractGasProvider, getDeploymentBinary(), encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<CertificateRegistry> deploy(Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit, String admin, String issuer) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, admin),
                new org.web3j.abi.datatypes.Address(160, issuer)));
        return deployRemoteCall(CertificateRegistry.class, web3j, credentials, gasPrice, gasLimit, getDeploymentBinary(), encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<CertificateRegistry> deploy(Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit,
            String admin, String issuer) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, admin),
                new org.web3j.abi.datatypes.Address(160, issuer)));
        return deployRemoteCall(CertificateRegistry.class, web3j, transactionManager, gasPrice, gasLimit, getDeploymentBinary(), encodedConstructor);
    }

    public static void linkLibraries(List<Contract.LinkReference> references) {
        librariesLinkedBinary = linkBinaryWithReferences(BINARY, references);
    }

    private static String getDeploymentBinary() {
        if (librariesLinkedBinary != null) {
            return librariesLinkedBinary;
        } else {
            return BINARY;
        }
    }

    public static class CertificateRecord extends StaticStruct {
        public byte[] certificateHash;

        public BigInteger issuedAt;

        public BigInteger expiresAt;

        public Boolean revoked;

        public String issuer;

        public CertificateRecord(byte[] certificateHash, BigInteger issuedAt, BigInteger expiresAt,
                Boolean revoked, String issuer) {
            super(new org.web3j.abi.datatypes.generated.Bytes32(certificateHash),
                    new org.web3j.abi.datatypes.generated.Uint64(issuedAt),
                    new org.web3j.abi.datatypes.generated.Uint64(expiresAt),
                    new org.web3j.abi.datatypes.Bool(revoked),
                    new org.web3j.abi.datatypes.Address(160, issuer));
            this.certificateHash = certificateHash;
            this.issuedAt = issuedAt;
            this.expiresAt = expiresAt;
            this.revoked = revoked;
            this.issuer = issuer;
        }

        public CertificateRecord(Bytes32 certificateHash, Uint64 issuedAt, Uint64 expiresAt,
                Bool revoked, Address issuer) {
            super(certificateHash, issuedAt, expiresAt, revoked, issuer);
            this.certificateHash = certificateHash.getValue();
            this.issuedAt = issuedAt.getValue();
            this.expiresAt = expiresAt.getValue();
            this.revoked = revoked.getValue();
            this.issuer = issuer.getValue();
        }
    }

    public static class CertificateIssuedEventResponse extends BaseEventResponse {
        public byte[] certificateKey;

        public byte[] certificateHash;

        public String issuer;

        public BigInteger issuedAt;

        public BigInteger expiresAt;
    }

    public static class CertificateRevokedEventResponse extends BaseEventResponse {
        public byte[] certificateKey;

        public String revokedBy;

        public BigInteger revokedAt;
    }

    public static class DefaultAdminDelayChangeCanceledEventResponse extends BaseEventResponse {
    }

    public static class DefaultAdminDelayChangeScheduledEventResponse extends BaseEventResponse {
        public BigInteger newDelay;

        public BigInteger effectSchedule;
    }

    public static class DefaultAdminTransferCanceledEventResponse extends BaseEventResponse {
    }

    public static class DefaultAdminTransferScheduledEventResponse extends BaseEventResponse {
        public String newAdmin;

        public BigInteger acceptSchedule;
    }

    public static class RoleAdminChangedEventResponse extends BaseEventResponse {
        public byte[] role;

        public byte[] previousAdminRole;

        public byte[] newAdminRole;
    }

    public static class RoleGrantedEventResponse extends BaseEventResponse {
        public byte[] role;

        public String account;

        public String sender;
    }

    public static class RoleRevokedEventResponse extends BaseEventResponse {
        public byte[] role;

        public String account;

        public String sender;
    }
}
